import uuid
import asyncio
from typing import List, Optional
from collections import defaultdict

from fastapi import FastAPI, HTTPException
from fastapi.responses import StreamingResponse
from dotenv import load_dotenv

from scripts_data import SCRIPTS, SCRIPT_INDEX
from models import (
    ScriptBrief, ScriptDetail, ChatSession, InMemorySessionStore, Message,
    VoteDetail, VoteRequest, VoteResultResponse, VoteCount,
    CreateSessionRequest, CreateSessionResponse, SendMessageRequest, SendMessageResponse,
    NextChapterResponse,ScriptListResponse
)
from graph import build_graph, GraphState, build_llm, build_group_system_prompt, \
    generate_character_vote, debug_print_prompt, parse_group_output, get_all_ai_roles

load_dotenv()

app = FastAPI(title="剧本杀AI后端-12.7", version="4.0.1")
session_store = InMemorySessionStore()
graph = build_graph()


# ==========================================
# 1. 剧本与会话管理
# ==========================================

@app.get("/scripts", response_model=ScriptListResponse)
def list_scripts(keyword: Optional[str] = None):
    results = []
    for s in SCRIPTS:
        if keyword:
            kw = keyword.lower()
            if kw not in s["title"].lower() and kw not in s["desc"].lower():
                continue
        results.append(ScriptBrief(**s))
    return ScriptListResponse(scripts=results)


@app.get("/scripts/score", response_model=ScriptListResponse)
def list_scripts_by_score():
    results = [ScriptBrief(**s) for s in SCRIPTS]
    sorted_data = sorted(results, key=lambda x: x.score, reverse=True)

    return ScriptListResponse(scripts=sorted_data)


@app.get("/scripts/{script_id}", response_model=ScriptDetail)
def get_script_detail(script_id: str):
    script = SCRIPT_INDEX.get(script_id)
    if not script:
        raise HTTPException(status_code=404, detail="Script not found")
    return ScriptDetail(**script)


@app.post("/sessions", response_model=CreateSessionResponse)
def create_session(req: CreateSessionRequest):
    script = SCRIPT_INDEX.get(req.script_id)
    if not script:
        raise HTTPException(status_code=404, detail="Script not found")

    valid_role_ids = [c['id'] for c in script['characters']]
    if req.user_role_id not in valid_role_ids:
        raise HTTPException(status_code=400, detail=f"Role ID {req.user_role_id} is invalid.")

    session_id = str(uuid.uuid4())
    session = session_store.create_session(
        session_id=session_id,
        script_id=req.script_id,
        user_role_id=req.user_role_id,
        model_name=req.model_name,
    )
    print(f"[Session] 创建成功: {session_id} (Model: {req.model_name})")
    return CreateSessionResponse(
        session_id=session_id,
        script_id=req.script_id,
        current_chapter_index=0
    )


@app.get("/sessions", response_model=List[ChatSession])
def list_sessions():
    return session_store.list_sessions()


@app.delete("/sessions/{session_id}")
def delete_session(session_id: str):
    session_store.delete_session(session_id)
    return {"status": "ok"}


@app.post("/sessions/{session_id}/clear")
def clear_session(session_id: str):
    session_store.clear_history(session_id)
    return {"status": "ok"}


# ==========================================
# 2. 核心对话
# ==========================================

@app.post("/sessions/{session_id}/message", response_model=SendMessageResponse)
async def send_message(session_id: str, req: SendMessageRequest):
    """
    普通对话接口：调用 Graph，Graph内部现在是 One Call 逻辑
    """
    session = session_store.get_session(session_id)
    if not session:
        raise HTTPException(status_code=404, detail="Session not found")

    # 注意：LangGraph 的逻辑已经在 graph.py 中改为单次调用
    state = GraphState(session=session, user_message=req.content)
    result_state = await graph.ainvoke(state)

    updated_session = result_state["session"]
    new_messages = result_state["new_ai_messages"]
    session_store.update_session(updated_session)

    return SendMessageResponse(
        ai_messages=new_messages,
        full_history=updated_session.history,
        current_chapter_index=updated_session.current_chapter_index
    )


@app.post("/sessions/{session_id}/message/stream")
async def send_message_stream(session_id: str, req: SendMessageRequest):
    """
    流式接口实现：
    已修改为【一次调用流式返回】。
    大模型会一次性输出形如 "【角色A】: ...\n【角色B】: ..." 的内容。
    """
    session = session_store.get_session(session_id)
    if not session:
        raise HTTPException(status_code=404, detail="Session not found")

    script = SCRIPT_INDEX[session.script_id]
    user_role_name = next((c["name"] for c in script["characters"] if c["id"] == session.user_role_id), "Player")
    user_input_fmt = f"【{user_role_name}】： {req.content}"

    # 将用户消息加入历史
    session.history.append(Message(role="user", speaker="player", content=user_input_fmt))
    session_store.update_session(session)

    async def event_generator():
        from langchain_core.messages import SystemMessage, HumanMessage, AIMessage

        # 1. 构建群聊 Prompt
        system_prompt = build_group_system_prompt(script, session.user_role_id, session)

        messages = [SystemMessage(content=system_prompt)]
        for msg in session.history:
            if msg.role == "user":
                messages.append(HumanMessage(content=msg.content))
            elif msg.role == "ai":
                prefix = f"【{msg.speaker}】：" if msg.speaker else ""
                messages.append(AIMessage(content=f"{prefix}{msg.content}"))
            elif msg.role == "system":
                messages.append(SystemMessage(content=f"【剧情旁白】: {msg.content}"))

        debug_print_prompt("GROUP-STREAM", messages)

        # 2. 调用 LLM
        llm = build_llm(session.model_name)
        full_content = ""

        # 发送开始事件
        yield "event: start\ndata: [START]\n\n"

        try:
            async for chunk in llm.astream(messages):
                delta = chunk.content
                if delta:
                    full_content += delta
                    # 直接透传大模型生成的文本（包含【角色名】：前缀）
                    # 前端需要根据换行符和【】解析显示
                    yield f"data: {delta}\n\n"
        except Exception as e:
            print(f"[Stream Error] {e}")
            yield f"data: (Error: {str(e)})\n\n"

        # 【新增】打印流式完整回复，保持与非流式一致的控制台体验
        print(f"\n{'='*20} 流式响应完整内容 {'='*20}")
        print(full_content)
        print(f"{'='*60}\n")

        # 3. 后处理：解析完整文本并存入历史记录
        # 我们需要在后台解析出是哪些角色说了话，以便下次构建 Prompt 历史时使用
        new_msgs = parse_group_output(full_content)
        for msg in new_msgs:
            session.history.append(msg)
        session_store.update_session(session)

        yield "event: done\ndata: [DONE]\n\n"

    return StreamingResponse(event_generator(), media_type="text/event-stream")


# ==========================================
# 3. 流程控制
# ==========================================

@app.post("/sessions/{session_id}/next_chapter", response_model=NextChapterResponse)
def trigger_next_chapter(session_id: str):
    session = session_store.get_session(session_id)
    if not session:
        raise HTTPException(404, "Session not found")

    script = SCRIPT_INDEX[session.script_id]
    next_idx = session.current_chapter_index + 1
    chapters = script.get("chapters", [])

    if next_idx > len(chapters):
        return NextChapterResponse(
            chapter_index=session.current_chapter_index,
            title="剧本结束",
            narration="搜证结束，请投票。",
            discussion_question="点击投票按钮。",
            status="finished"
        )

    chapter_data = chapters[next_idx - 1]
    session.current_chapter_index = next_idx

    # 【新增】美观打印剧情和问题到控制台
    print(f"\n{'='*20} 开启新章节: 第 {next_idx} 章 {'='*20}")
    print(f"【标题】: {chapter_data['title']}")
    print(f"【剧情】: {chapter_data['narration']}")
    print(f"【问题】: {chapter_data['discussion_question']}")
    print(f"{'='*60}\n")

    narrator_text = (
        f"【第{chapter_data['chapter_id']}章：{chapter_data['title']}】\n"
        f"{chapter_data['narration']}\n"
        f"【问题】：{chapter_data['discussion_question']}"
    )

    sys_msg = Message(role="system", speaker="旁白", content=narrator_text)
    session.history.append(sys_msg)
    session_store.update_session(session)

    return NextChapterResponse(
        chapter_index=next_idx,
        title=chapter_data['title'],
        narration=chapter_data['narration'],
        discussion_question=chapter_data['discussion_question'],
        status="ongoing"
    )


@app.post("/sessions/{session_id}/vote", response_model=VoteResultResponse)
async def vote_and_reveal(session_id: str, req: VoteRequest):
    print(f"[Vote] 收到投票请求: {req.target_role_id}")
    session = session_store.get_session(session_id)
    if not session:
        raise HTTPException(404, "Session not found")

    script = SCRIPT_INDEX[session.script_id]

    target_char = next((c for c in script["characters"] if c["id"] == req.target_role_id), None)
    if not target_char:
        raise HTTPException(status_code=400, detail="投票失败：目标角色不存在")

    # 使用 graph.py 中重新加回的函数，确保代码简洁统一
    ai_roles = get_all_ai_roles(script, session.user_role_id)

    tasks = [generate_character_vote(session, rid) for rid in ai_roles]
    ai_votes_details = await asyncio.gather(*tasks)

    tally = defaultdict(int)

    user_target_name = target_char["name"]
    tally[user_target_name] += 1

    for vote in ai_votes_details:
        # 清理可能产生的标点
        name = vote.target_role_name.strip().replace("。", "").replace("【", "").replace("】", "")
        tally[name] += 1

    vote_counts = [VoteCount(role_name=k, count=v) for k, v in tally.items()]
    vote_counts.sort(key=lambda x: x.count, reverse=True)

    # 【新增】投票结果美观打印
    print(f"\n{'='*25} 投票结果公示 {'='*25}")
    print(f"真凶揭秘: 【{script.get('truth', '未知')}】")
    print("-" * 60)
    print("【得票统计】")
    for vc in vote_counts:
        print(f"  * {vc.role_name}: {vc.count} 票")
    print("-" * 60)
    print("【详细投票理由】")
    print(f"  [玩家] 投给了 -> {user_target_name}")
    for v in ai_votes_details:
        print(f"  [{v.voter_name}] 投给了 -> {v.target_role_name}")
        print(f"      └── 理由: {v.reasoning}")
    print(f"{'='*60}\n")

    return VoteResultResponse(
        vote_counts=vote_counts,
        ai_votes=ai_votes_details,
        truth=script.get("truth", "真相未定义")
    )


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=9668)