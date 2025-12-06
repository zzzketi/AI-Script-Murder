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
    NarratageResponse, NextChapterResponse
)
from graph import build_graph, GraphState, build_llm, build_role_system_prompt, get_all_ai_roles, \
    generate_character_vote, debug_print_prompt

load_dotenv()

app = FastAPI(title="剧本杀AI后端-SiliconFlow完整版", version="3.5.0")
session_store = InMemorySessionStore()
graph = build_graph()


# ==========================================
# 1. 剧本与会话管理
# ==========================================

@app.get("/scripts", response_model=List[ScriptBrief])
def list_scripts(keyword: Optional[str] = None):
    results = []
    for s in SCRIPTS:
        if keyword:
            kw = keyword.lower()
            if kw not in s["title"].lower() and kw not in s["description"].lower():
                continue
        results.append(ScriptBrief(**s))
    return results


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
    session = session_store.get_session(session_id)
    if not session:
        raise HTTPException(status_code=404, detail="Session not found")

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
    循环遍历所有 AI 角色，为每个角色分配独立的大模型上下文进行调用。
    """
    session = session_store.get_session(session_id)
    if not session:
        raise HTTPException(status_code=404, detail="Session not found")

    script = SCRIPT_INDEX[session.script_id]
    user_role_name = next((c["name"] for c in script["characters"] if c["id"] == session.user_role_id), "Player")
    user_input_fmt = f"【{user_role_name}】： {req.content}"
    session.history.append(Message(role="user", speaker="player", content=user_input_fmt))
    session_store.update_session(session)

    ai_roles = get_all_ai_roles(script, session.user_role_id)

    async def event_generator():
        if not ai_roles:
            yield "data: [No AI roles]\n\n"
            return

        from langchain_core.messages import SystemMessage, HumanMessage, AIMessage

        for i, role_id in enumerate(ai_roles):
            role_info = next((c for c in script["characters"] if c["id"] == role_id), {})
            role_name = role_info.get('name', 'AI')

            # 角色间隔，避免高并发
            if i > 0:
                await asyncio.sleep(1.5)
            else:
                await asyncio.sleep(0.5)

            # 【重要】为当前角色构建专属的 System Prompt
            system_prompt = build_role_system_prompt(
                script, role_id, role_info, session
            )

            messages = [SystemMessage(content=system_prompt)]
            for msg in session.history:
                if msg.role == "user":
                    messages.append(HumanMessage(content=msg.content))
                elif msg.role == "ai":
                    messages.append(AIMessage(content=msg.content))
                elif msg.role == "system":
                    messages.append(SystemMessage(content=f"【剧情旁白】: {msg.content}"))

            # 打印当前角色的完整Prompt (无截断)
            debug_print_prompt(f"{role_name}-STREAM", messages)
            yield f"event: role_info\ndata: {role_name}\n\n"

            # 重新实例化LLM，确保全新的调用上下文
            llm = build_llm(session.model_name)
            full_content = ""

            try:
                retry_count = 0
                max_retries = 3
                success = False

                while retry_count < max_retries and not success:
                    if retry_count > 0:
                        await asyncio.sleep(2)
                        yield f"data: ...\n\n"

                    temp_content = ""
                    async for chunk in llm.astream(messages):
                        delta = chunk.content
                        if delta:
                            temp_content += delta
                            yield f"data: {delta}\n\n"

                    if len(temp_content.strip()) > 0:
                        full_content = temp_content
                        success = True
                    else:
                        print(f"[Stream Warning] {role_name} 返回空，重试 {retry_count + 1}")
                        retry_count += 1

                if not success:
                    fallback_text = "..."
                    yield f"data: {fallback_text}\n\n"
                    full_content = fallback_text

                ai_msg = Message(role="ai", speaker=role_name, content=full_content)
                session.history.append(ai_msg)
                session_store.update_session(session)

                yield f"event: end_role\ndata: {role_name}\n\n"

            except Exception as e:
                print(f"[Stream Error] {e}")
                yield f"data: (Error: {str(e)})\n\n"

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

    print(f"[Chapter] 开启第 {next_idx} 章: {chapter_data['title']}")
    # 打印完整剧情和问题
    print(f"【剧情】: {chapter_data['narration']}")
    print(f"【问题】: {chapter_data['discussion_question']}")

    # 旁白与问题之间不空行
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

    ai_roles = get_all_ai_roles(script, session.user_role_id)
    # 并发投票，generate_character_vote 内部有独立的 LLM 调用和 Prompt
    tasks = [generate_character_vote(session, rid) for rid in ai_roles]
    ai_votes_details = await asyncio.gather(*tasks)

    tally = defaultdict(int)

    user_target_name = target_char["name"]
    tally[user_target_name] += 1

    for vote in ai_votes_details:
        name = vote.target_role_name.strip().replace("。", "").replace("【", "").replace("】", "")
        tally[name] += 1

    vote_counts = [VoteCount(role_name=k, count=v) for k, v in tally.items()]
    vote_counts.sort(key=lambda x: x.count, reverse=True)

    return VoteResultResponse(
        vote_counts=vote_counts,
        ai_votes=ai_votes_details,
        truth=script.get("truth", "真相未定义")
    )


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8129)