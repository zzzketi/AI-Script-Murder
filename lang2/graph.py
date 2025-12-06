import os
import asyncio
from typing import List, Dict, Any, Optional
from dotenv import load_dotenv

from langgraph.graph import StateGraph, END
from langchain_openai import ChatOpenAI
from langchain_core.messages import SystemMessage, HumanMessage, AIMessage, BaseMessage
from pydantic import BaseModel

from models import ChatSession, Message, VoteDetail
from scripts_data import SCRIPT_INDEX

load_dotenv()


# ==========================================
# 1. Graph State 定义
# ==========================================
class GraphState(BaseModel):
    session: ChatSession
    user_message: str
    current_role_id: Optional[str] = None
    remaining_roles: List[str] = []
    new_ai_messages: List[Message] = []
    next_node: str = "END"


# ==========================================
# 2. 辅助工具函数
# ==========================================

def build_llm(model_name: str) -> ChatOpenAI:
    sf_api_key = "sk-ritmaymesqwqcicgozyepihyatollycwlkgyuwdlqfqtyrgs"
    sf_base_url = "https://api.siliconflow.cn/v1"
    target_model = model_name if model_name else "Qwen/Qwen2.5-72B-Instruct-128K"

    return ChatOpenAI(
        model=target_model,
        temperature=0.2,
        api_key=sf_api_key,
        base_url=sf_base_url,
        streaming=True,
        max_retries=3,
        request_timeout=60
    )


def debug_print_prompt(role_name: str, messages: List[BaseMessage]):
    """控制台打印 Prompt - 【修改】移除截断，打印完整内容"""
    print(f"\n{'=' * 25} PROMPT DEBUG ({role_name}) {'=' * 25}")
    for msg in messages:
        if isinstance(msg, SystemMessage):
            # 满足【SYSTEM输出前空两行】的需求
            print("\n\n")
            print(f"[SYSTEM]: {msg.content}")
        elif isinstance(msg, HumanMessage):
            print(f"[USER]: {msg.content}")
        elif isinstance(msg, AIMessage):
            print(f"[AI]: {msg.content}")
    print(f"{'=' * 65}\n")


def build_role_system_prompt(script: dict, role_id: str, role_info: dict, session: ChatSession) -> str:
    """构建 System Prompt"""
    role_name = role_info.get("name", "未命名角色")
    public_info = role_info.get("public_info", "无")
    secret_info = role_info.get("secret_info", "无")
    script_title = script.get("title", "")

    chapter_context = ""
    idx = session.current_chapter_index
    if 1 <= idx <= 5:
        chapters = script.get("chapters", [])
        if len(chapters) >= idx:
            chap = chapters[idx - 1]
            chapter_context = (
                f"【当前进行章节】{chap['title']}\n"
                f"   【当前章节旁白】{chap['narration']}\n"
                f"   【当前讨论焦点】{chap['discussion_question']}\n"
            )
    elif idx == 0:
        chapter_context = "【当前状态】游戏尚未开始，等待第一章开启。"
    else:
        chapter_context = "【当前状态】剧情结束，复盘阶段。"

    prompt = f"""
    你正在进行剧本杀《{script_title}》,现在轮到你进行回复，你需要根据以下人设、剧情和历史消息记录进行回复，你仅能以当前扮演角色发言，要求发言最开始强制以【{role_name}】：开头（重要！！！！！！！！！！！！！）。
    你不需要重复别人的话，只需要按照当前角色设定，根据历史对话顺序进行回复

    【你的角色】
    名字：{role_name}
    公开人设：{public_info}
    秘密信息：{secret_info}

    {chapter_context}

    【指令】
    1. 必须完全代入角色，不要跳戏。
    2. 如果你是凶手，请结合【秘密信息】撒谎掩盖。
    3. 如果你是好人，请根据剧情逻辑推理。
    4. 回复简短，像真人在聊天。

    【回复内容格式（重要！！！！！！！！！！！！！！必须严格按照！！！！！！！！！！！）】
    1. 回复的最开始不得加回车等换行符号。
    2. 你必须回复，可以适当反问问题。

    【历史对话记录】
    """
    return prompt.strip()


def get_all_ai_roles(script: dict, user_role_id: str) -> List[str]:
    """获取AI角色ID列表"""
    ai_roles = []
    for character in script.get("characters", []):
        role_id = character.get("id", "")
        if role_id != user_role_id and role_id:
            ai_roles.append(role_id)
    return ai_roles


# ==========================================
# 3. LangGraph 节点定义
# ==========================================

async def process_user_input(state: GraphState) -> Dict[str, Any]:
    session = state.session
    script = SCRIPT_INDEX[session.script_id]
    role_name = next((c["name"] for c in script["characters"] if c["id"] == session.user_role_id), "Player")

    input_message = f"【{role_name}】： {state.user_message}"
    print(f"\n[玩家Input] :{input_message}")

    user_msg = Message(role="user", speaker="player", content=input_message)
    session.history.append(user_msg)

    ai_roles = get_all_ai_roles(script, session.user_role_id)
    # 如果有AI角色，则进入角色发言节点，否则直接结束
    next_node = "role_node" if ai_roles else "END"

    return {
        "session": session,
        "remaining_roles": ai_roles[1:] if ai_roles else [],
        "current_role_id": ai_roles[0] if ai_roles else None,
        "new_ai_messages": [],
        "next_node": next_node
    }


async def role_speaking_step(state: GraphState) -> Dict[str, Any]:
    """
    角色发言节点。
    在此处实现了【每个角色一个大模型对应调用】：
    我们根据 current_role_id 动态生成专属的 system prompt，
    然后发起独立的 LLM 调用。
    """
    if not state.current_role_id:
        return {"next_node": "END"}

    session = state.session
    script = SCRIPT_INDEX[session.script_id]
    current_role_id = state.current_role_id
    role_info = next((c for c in script["characters"] if c["id"] == current_role_id), None)

    # 1. 独立构建该角色的 Prompt (相当于加载了该角色的“人格模型”)
    system_prompt = build_role_system_prompt(script, current_role_id, role_info, session)

    messages = [SystemMessage(content=system_prompt)]
    for msg in session.history:
        if msg.role == "user":
            messages.append(HumanMessage(content=msg.content))
        elif msg.role == "ai":
            messages.append(AIMessage(content=msg.content))
        elif msg.role == "system":
            messages.append(SystemMessage(content=f"【剧情旁白】: {msg.content}"))

    # 打印完整的 prompt，不做截断
    debug_print_prompt(role_info['name'], messages)

    llm = build_llm(session.model_name)
    content = ""

    # ==================== 稳健性重试逻辑 ====================
    max_retries = 3
    for attempt in range(max_retries):
        try:
            wait_time = 1.5 + (attempt * 1)
            print(f"[{role_info['name']}] 等待 {wait_time}s 后开始思考 (Try {attempt + 1})...")
            await asyncio.sleep(wait_time)

            response = await llm.ainvoke(messages)
            content = response.content

            if content and len(content.strip()) > 0:
                break
            else:
                print(f"[Warning] {role_info['name']} 返回空白，重试中...")

        except Exception as e:
            print(f"[Error] LLM 调用失败: {e}")
            if attempt == max_retries - 1:
                content = f"（{role_info['name']} 似乎在沉思，没有说话。）"
    # =======================================================

    print(f"[Output]: {content}\n")

    ai_msg = Message(
        role="ai",
        speaker=role_info.get("name", "Unknown"),
        content=content
    )
    session.history.append(ai_msg)

    next_role = None
    next_remaining = state.remaining_roles
    next_dest = "role_node"

    if state.remaining_roles:
        next_role = state.remaining_roles[0]
        next_remaining = state.remaining_roles[1:]
    else:
        next_dest = "END"

    return {
        "session": session,
        "new_ai_messages": state.new_ai_messages + [ai_msg],
        "current_role_id": next_role,
        "remaining_roles": next_remaining,
        "next_node": next_dest
    }


def build_graph():
    graph = StateGraph(GraphState)
    graph.add_node("process_input", process_user_input)
    graph.add_node("role_node", role_speaking_step)

    graph.set_entry_point("process_input")

    graph.add_conditional_edges("process_input", lambda s: s.next_node, {"role_node": "role_node", "END": END})
    graph.add_conditional_edges("role_node", lambda s: s.next_node, {"role_node": "role_node", "END": END})

    return graph.compile()


# ==========================================
# 4. 独立投票功能
# ==========================================

async def generate_character_vote(session: ChatSession, role_id: str) -> VoteDetail:
    script = SCRIPT_INDEX[session.script_id]
    role_info = next((c for c in script["characters"] if c["id"] == role_id), None)

    prompt = f"""
    你是《{script['title']}》中的角色【{role_info['name']}】。
    现在到了【最终投票环节】。
    请回顾下方所有历史剧情和对话，找出真凶。
    即使你是凶手，也要假装投票给别人。

    **格式要求**（严格遵守）：
    指认对象：XXX
    理由：简短理由
    """

    messages = [SystemMessage(content=prompt)]
    for msg in session.history:
        prefix = msg.speaker if msg.speaker else "旁白"
        messages.append(HumanMessage(content=f"{prefix}: {msg.content}"))

    llm = build_llm(session.model_name)
    target = "弃票"
    reason = "API调用失败"

    max_retries = 3
    for attempt in range(max_retries):
        try:
            await asyncio.sleep(2 + (attempt * 1.5))

            resp = await llm.ainvoke(messages)
            text = resp.content

            if text and len(text.strip()) > 0:
                current_target = None
                current_reason = None
                for line in text.split('\n'):
                    if "指认对象" in line or "对象" in line:
                        parts = line.split("：")
                        if len(parts) > 1: current_target = parts[1].strip()
                    if "理由" in line:
                        parts = line.split("：")
                        if len(parts) > 1: current_reason = parts[1].strip()

                if current_target:
                    target = current_target
                    reason = current_reason if current_reason else "理由未明确"
                    break
        except Exception as e:
            print(f"[Vote Error] {role_info['name']} 投票失败: {e}")

    return VoteDetail(
        voter_role_id=role_id,
        voter_name=role_info['name'],
        target_role_name=target,
        reasoning=reason
    )