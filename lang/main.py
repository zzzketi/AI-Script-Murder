# main.py
import uuid
from typing import List, Optional

from fastapi import FastAPI, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel

from scripts_data import SCRIPTS, SCRIPT_INDEX, AUTHOR_INDEX
from models import (
    ScriptBrief,
    ScriptDetail,
    Author,
    Character,
    ChatSession,
    InMemorySessionStore,
    Message,
)
from graph import build_graph, GraphState

app = FastAPI(title="剧本杀AI后端示例", version="0.1.0")
session_store = InMemorySessionStore()
graph = build_graph()


# ========= 剧本相关接口（P0） =========

class ScriptListResponse(BaseModel):
    scripts: List[ScriptBrief]


@app.get("/scripts", response_model=ScriptListResponse)
def list_scripts(keyword: Optional[str] = None):
    """剧本列表 + 搜索功能"""
    results = []
    for s in SCRIPTS:
        if keyword:
            kw = keyword.lower()
            if kw not in s["title"].lower() and kw not in s["description"].lower():
                continue
        results.append(
            ScriptBrief(
                id=s["id"],
                title=s["title"],
                cover_url=s["cover_url"],
                description=s["description"],
            )
        )
    return ScriptListResponse(scripts=results)


@app.get("/scripts/{script_id}", response_model=ScriptDetail)
def get_script_detail(script_id: str):
    script = SCRIPT_INDEX.get(script_id)
    if not script:
        raise HTTPException(status_code=404, detail="Script not found")

    author = Author(**script["author"])
    characters = [Character(**c) for c in script["characters"]]

    return ScriptDetail(
        id=script["id"],
        title=script["title"],
        cover_url=script["cover_url"],
        description=script["description"],
        background=script["background"],
        author=author,
        characters=characters,
    )


# 可选：作者详情（P1）
class AuthorDetailResponse(BaseModel):
    id: str
    name: str
    avatar: str
    bio: str
    scripts: List[ScriptBrief]


@app.get("/authors/{author_id}", response_model=AuthorDetailResponse)
def get_author_detail(author_id: str):
    author = AUTHOR_INDEX.get(author_id)
    if not author:
        raise HTTPException(status_code=404, detail="Author not found")
    return AuthorDetailResponse(
        id=author["id"],
        name=author["name"],
        avatar=author["avatar"],
        bio=author["bio"],
        scripts=[
            ScriptBrief(**s) for s in author["scripts"]
        ],
    )


# ========= 会话相关接口（P0, P1） =========

class CreateSessionRequest(BaseModel):
    script_id: str
    user_role_id: str  # 用户在剧本中选择的角色 ID
    model_name: str = "gpt-4o-mini"  # 你可以允许前端切换不同模型（P1）


class CreateSessionResponse(BaseModel):
    session_id: str
    script_id: str
    user_role_id: str
    model_name: str


@app.post("/sessions", response_model=CreateSessionResponse)
def create_session(req: CreateSessionRequest):
    if req.script_id not in SCRIPT_INDEX:
        raise HTTPException(status_code=404, detail="Script not found")

    script = SCRIPT_INDEX[req.script_id]
    role_ids = [c["id"] for c in script["characters"]]
    if req.user_role_id not in role_ids:
        raise HTTPException(status_code=400, detail="Invalid user_role_id")

    session_id = str(uuid.uuid4())
    session_store.create_session(
        session_id=session_id,
        script_id=req.script_id,
        user_role_id=req.user_role_id,
        model_name=req.model_name,
    )
    return CreateSessionResponse(
        session_id=session_id,
        script_id=req.script_id,
        user_role_id=req.user_role_id,
        model_name=req.model_name,
    )


class HistoryResponse(BaseModel):
    session_id: str
    script_id: str
    user_role_id: str
    model_name: str
    history: List[Message]


@app.get("/sessions/{session_id}/history", response_model=HistoryResponse)
def get_history(session_id: str):
    session = session_store.get_session(session_id)
    if not session:
        raise HTTPException(status_code=404, detail="Session not found")
    return HistoryResponse(
        session_id=session.session_id,
        script_id=session.script_id,
        user_role_id=session.user_role_id,
        model_name=session.model_name,
        history=session.history,
    )


class SendMessageRequest(BaseModel):
    content: str


class SendMessageResponse(BaseModel):
    ai_messages: List[Message]
    full_history: List[Message]


@app.post("/sessions/{session_id}/message", response_model=SendMessageResponse)
async def send_message(session_id: str, req: SendMessageRequest):
    session = session_store.get_session(session_id)
    if not session:
        raise HTTPException(status_code=404, detail="Session not found")

    state = GraphState(session=session, user_message=req.content)
    result_state = await graph.ainvoke(state)

    session_after = result_state["session"]
    new_ai_messages = result_state["new_ai_messages"]

    session_store.update_session(session_after)

    return SendMessageResponse(
        ai_messages=new_ai_messages,
        full_history=session_after.history,
    )


# ========= 流式输出示例接口（P0：流式打字机效果） =========

@app.post("/sessions/{session_id}/message/stream")
async def send_message_stream(session_id: str, req: SendMessageRequest):
    """
    演示流式输出接口。
    实际上我们会在 LangGraph 里面流式收 token，这里做一个简单包装：
    使用 text/event-stream (SSE) 形式返回。
    Android 侧如果不方便 SSE，也可以改成 WebSocket。
    """
    session = session_store.get_session(session_id)
    if not session:
        raise HTTPException(status_code=404, detail="Session not found")

    state = GraphState(session=session, user_message=req.content)

    async def event_generator():
        # 这里我们复用之前的 multi_role_step 逻辑，
        # 但是为了清晰起见，可以在 graph.py 里再写一个专门的 streaming 节点。
        from graph import build_llm, build_system_prompt
        from langchain_core.messages import SystemMessage, HumanMessage, AIMessage
        from scripts_data import SCRIPT_INDEX
        from datetime import datetime

        session = state.session
        script = SCRIPT_INDEX[session.script_id]
        user_role_id = session.user_role_id

        llm = build_llm(session.model_name)
        system_prompt = build_system_prompt(script, user_role_id)

        lc_messages = [SystemMessage(content=system_prompt)]
        for msg in session.history:
            if msg.role == "user":
                lc_messages.append(HumanMessage(content=f"[玩家] {msg.content}"))
            else:
                lc_messages.append(AIMessage(content=msg.content))

        lc_messages.append(HumanMessage(content=f"[玩家] {state.user_message}"))

        full_reply = ""
        async for chunk in llm.astream(lc_messages):
            delta = chunk.content or ""
            full_reply += delta
            # SSE 一般每次发送一行 "data: xxx\n\n"
            yield f"data: {delta}\n\n"

        # 结束时，把完整回复写入会话历史
        lines = [ln.strip() for ln in full_reply.split("\n") if ln.strip()]
        new_ai_messages: List[Message] = []
        now = datetime.utcnow()
        for line in lines:
            speaker = None
            content = line
            if line.startswith("[") and "]" in line:
                closing = line.find("]")
                speaker = line[1:closing].strip()
                content = line[closing + 1 :].strip()
            new_ai_messages.append(
                Message(role="ai", speaker=speaker, content=content, timestamp=now)
            )

        user_msg = Message(role="user", speaker="player", content=req.content)
        session.history.append(user_msg)
        session.history.extend(new_ai_messages)
        session_store.update_session(session)

        # 发送一个结束标记
        yield "event: done\ndata: [DONE]\n\n"

    return StreamingResponse(event_generator(), media_type="text/event-stream")


# ========= 会话管理：清空 / 删除 / 列表（P0 + P1） =========

@app.post("/sessions/{session_id}/clear")
def clear_session_history(session_id: str):
    session = session_store.get_session(session_id)
    if not session:
        raise HTTPException(status_code=404, detail="Session not found")
    session_store.clear_history(session_id)
    return {"status": "ok"}


class SessionBrief(BaseModel):
    session_id: str
    script_id: str
    script_title: str
    last_message: Optional[str]


@app.get("/sessions", response_model=List[SessionBrief])
def list_all_sessions():
    """
    简单的会话记录列表（P1：类似微信首页聊天列表）
    """
    sessions = session_store.list_sessions()
    brs: List[SessionBrief] = []
    for s in sessions:
        last_content = s.history[-1].content if s.history else None
        script = SCRIPT_INDEX.get(s.script_id)
        script_title = script["title"] if script else s.script_id
        brs.append(
            SessionBrief(
                session_id=s.session_id,
                script_id=s.script_id,
                script_title=script_title,
                last_message=last_content,
            )
        )
    return brs


@app.delete("/sessions/{session_id}")
def delete_session(session_id: str):
    session_store.delete_session(session_id)
    return {"status": "ok"}


# ======== 启动命令 ========
# 在命令行运行：
#   uvicorn main:app --reload --port 8000
#
# 然后在浏览器打开：
#   http://127.0.0.1:8000/docs
# 即可看到自动生成的 Swagger 文档并测试接口。