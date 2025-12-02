# graph.py
from typing import List, Dict, Any
from langgraph.graph import StateGraph, END
from langchain_openai import ChatOpenAI
from pydantic import BaseModel
from datetime import datetime

from models import ChatSession, Message, ScriptDetail
from scripts_data import get_script_detail


class GraphState(BaseModel):
    """LangGraph 中流动的状态"""
    session: ChatSession
    user_message: str
    # 让节点能把生成的AI消息追加回去
    new_ai_messages: List[Message] = []


def build_llm(model_name: str) -> ChatOpenAI:
    # 这里示例使用 OpenAI，你可根据需要切换成其他模型
    return ChatOpenAI(
        model=model_name,
        temperature=0.7,
        streaming=True,  # 为流式输出做准备
    )


def build_system_prompt(script: ScriptDetail, user_role_id: str) -> str:
    # 说明多角色群聊设定
    bg = script.background
    background_text = bg.story
    roles_desc = []
    for ch in script.characters:
        roles_desc.append(f"- {ch.id}（{ch.name}）：{ch.desc}")
    roles_text = "\n".join(roles_desc)

    return f"""
你是一套「剧本杀群聊控制器」，负责同时扮演除玩家角色外的所有角色。

【剧本背景】：
{background_text}

【剧本角色】：
{roles_text}

【玩家设置】：
- 玩家当前扮演的角色 id: {user_role_id}
- 你不能替玩家说话，只能扮演其他角色。
- 回复形式需要模拟群聊：一次用户发言后，你可以让 1~3 个不同角色轮流发言。
- 每一条回复中，必须标注「说话角色」用于前端展示，例如：
  [刑警·周明] 我认为案发时间可能并不是大家以为的那样……
  [乘客·林医生] 我刚才一直在车厢里，没有离开。

【对话目标】：
- 通过与玩家的对话，引导玩家一步步接近真相；
- 合理地隐瞒或逐步透露线索；
- 始终保持人设和剧情逻辑自洽。

【输出要求】：
- 用简体中文。
- 你的输出会被前端按行拆分，每一行视作一个角色的一句话。
- 不要输出 Markdown，只需要普通文本。
- 不要复述系统提示，直接以角色发言开始。
    """.strip()


async def multi_role_step(state: GraphState) -> GraphState:
    """
    单个图节点：根据当前会话状态和用户消息，生成多角色轮流发言。
    """
    session = state.session
    script = get_script_detail(session.script_id)
    if not script:
        # 简单容错
        script = ScriptDetail(
            id="error", title="error", desc="", image="", score=0, difficulty="", tags=[],
            background=None, system_prompt="", characters=[]
        )  # 或者直接 raise Exception

    user_role_id = session.user_role_id

    # 准备 LLM
    llm = build_llm(session.model_name)

    system_prompt = build_system_prompt(script, user_role_id)

    # 整理历史对话 -> LangChain message 格式
    from langchain_core.messages import SystemMessage, HumanMessage, AIMessage

    lc_messages = [SystemMessage(content=system_prompt)]
    for msg in session.history:
        if msg.role == "user":
            lc_messages.append(HumanMessage(content=f"[玩家] {msg.content}"))
        else:
            # AI 消息已经在 content 中嵌有角色标记
            lc_messages.append(AIMessage(content=msg.content))

    # 当前用户发言
    lc_messages.append(HumanMessage(content=f"[玩家] {state.user_message}"))

    # 调用 LLM（流式）
    full_reply = ""

    async for chunk in llm.astream(lc_messages):
        delta = chunk.content or ""
        full_reply += delta
        # 在这里你可以把 delta 通过 WebSocket 推给前端，实现「打字机」效果

    # 将 AI 回复按行拆分，每一行视为一个角色的一句话
    lines = [ln.strip() for ln in full_reply.split("\n") if ln.strip()]
    new_ai_messages: List[Message] = []
    now = datetime.utcnow()
    for line in lines:
        # 简单解析形如：[刑警·周明] 内容
        speaker = None
        content = line
        if line.startswith("[") and "]" in line:
            closing = line.find("]")
            speaker = line[1:closing].strip()
            content = line[closing + 1 :].strip()

        new_ai_messages.append(
            Message(
                role="ai",
                speaker=speaker,
                content=content,
                timestamp=now,
            )
        )

    # 将用户消息与 AI 消息追加到会话历史
    user_msg = Message(role="user", speaker="player", content=state.user_message)
    session.history.append(user_msg)
    session.history.extend(new_ai_messages)

    # 更新状态
    state.session = session
    state.new_ai_messages = new_ai_messages
    return state


def build_graph():
    """
    构建一个只有单节点的简单图：
    user_message -> multi_role_step -> END
    """
    graph = StateGraph(GraphState)
    graph.add_node("multi_role", multi_role_step)
    graph.set_entry_point("multi_role")
    graph.set_finish_point("multi_role")
    return graph.compile()