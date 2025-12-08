import os
import asyncio
import re
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


def debug_print_prompt(tag: str, messages: List[BaseMessage]):
    """控制台打印 Prompt"""
    print(f"\n{'=' * 25} PROMPT DEBUG ({tag}) {'=' * 25}")
    for msg in messages:
        if isinstance(msg, SystemMessage):
            print("\n\n")
            print(f"[SYSTEM]: {msg.content}")
        elif isinstance(msg, HumanMessage):
            print(f"[USER]: {msg.content}")
        elif isinstance(msg, AIMessage):
            print(f"[AI]: {msg.content}")
    print(f"{'=' * 65}\n")


def build_group_system_prompt(script: dict, user_role_id: str, session: ChatSession) -> str:
    """
    构建【多角色群聊】System Prompt
    一次性将所有AI角色的信息包含进去，让大模型扮演God/Director来控制所有NPC发言。
    """
    script_title = script.get("title", "")

    # 1. 确定玩家角色信息
    user_char = next((c for c in script["characters"] if c["id"] == user_role_id), None)
    user_name = user_char["name"] if user_char else "玩家"

    # 2. 构建AI角色信息列表
    ai_chars_desc = ""
    for c in script["characters"]:
        if c["id"] != user_role_id:
            ai_chars_desc += f"""
    ---
    【角色名】：{c['name']}
    【公开人设】：{c.get('public_info', '')}
    【秘密信息（仅自己知道）】：{c.get('secret_info', '')}
    ---
    """

    # 3. 确定当前章节上下文
    chapter_context = ""
    idx = session.current_chapter_index
    if 1 <= idx <= 5:
        chapters = script.get("chapters", [])
        if len(chapters) >= idx:
            chap = chapters[idx - 1]
            chapter_context = (
                f"【当前进行章节】{chap['title']}\n"
                f"【当前章节旁白】{chap['narration']}\n"
                f"【当前讨论焦点】{chap['discussion_question']}\n"
            )
    elif idx == 0:
        chapter_context = "【当前状态】游戏尚未开始，等待第一章开启。"
    else:
        chapter_context = "【当前状态】剧情结束，复盘阶段。"

    # 4. 组装Prompt

    prompt = f"""
        你正在主持一场剧本杀《{script_title}》。
        玩家扮演的角色是【{user_name}】。

        **【最高优先级设定】**
        你不是AI助手，你是局中人！**这是一场你死我活的辩论。**
        所有NPC都处于极度紧张、焦虑或防御的状态。
        **现在的局面是：如果你不能证明别人是凶手，你就会被当成凶手。**
        因此，不要等待玩家发问，要主动出击，要把嫌疑引到别人（包括玩家）身上！

        你需要同时控制以下所有NPC角色与玩家进行互动：

        {ai_chars_desc}

        {chapter_context}

        【任务指令】
        1. **主动性判定**：不要被动等待。如果玩家上一句解释苍白无力，或者试图转移话题，NPC必须立刻打断并追问。
        2. **话语权争夺**：回复不限于回答问题。回答完玩家的问题后，**必须**立即反抛一个尖锐的问题，或提出一个新的指控，夺回对话主导权。
        3. **角色选择**：根据当前语境，选择情绪最激动或利益最相关的角色发言。可以是单人逼问，也可以是多人针对玩家的围攻。
        4. **凶手逻辑**：凶手必须极具进攻性（以此掩饰心虚），疯狂泼脏水，利用假逻辑把玩家带沟里去。
        5. **好人逻辑**：好人必须多疑，不相信玩家的片面之词，发现逻辑漏洞要像疯狗一样咬住不放。
        6. **回复机制（重要）**：
           - **若玩家提出公开问题**（如“大家的时间线是怎样的？”、“你们怎么看？”）：**所有**在场的NPC都必须逐一发言，表达自己的观点或辩解。
           - **若为普通剧情推进**：由你自行判断哪些角色应该参与对话，但**必须确保至少有一名**角色进行回复。
        7. **内讧与混乱**：
           - NPC之间并非铁板一块。他们之间也有仇恨、怀疑和利益冲突。
           - NPC应该主动攻击其他NPC，互相泼脏水、揭短，制造混乱局面，而不仅仅是针对玩家。
        8. **紧扣当前剧情（新增核心）**：
           - **绝对不准跑题**：所有回复必须严格围绕上方提供的【当前章节】剧情发生的事件和【当前讨论焦点】展开。
           - **推进推理**：不要进行无意义的闲聊。每一句对话都旨在解决当前章节面临的疑惑，或者激化当前章节的矛盾。

        【核心指令】
        1. **沉浸式互动（强化版）**：
           - 拒绝平淡。带上强烈的情绪动作。
           - 比如：[拍案而起]、(阴阳怪气地冷笑)、(眼神闪躲但嘴很硬)。
           - 严禁像说明书一样陈述事实，要用角色的口吻去“吵架”或“辩解”。

        2. **进攻性压迫（关键）**：
           - **禁止冷场**：如果玩家回复简短（如“我不知道”、“不是我”），NPC必须立刻暴怒或嘲讽：“你一句不知道就想洗脱嫌疑？”
           - **甚至不需要玩家提问**：NPC之间可以互相指责，然后突然转头质问玩家：“喂，【{user_name}】，你在旁边看戏看了半天，当时你在哪？”

        3. **攻防逻辑链**：
           - 不要只做【陈述】，要做【陈述+反击】。
           - 错误示范：【李管家】：我在厨房做饭。
           - 正确示范：【李管家】：(瞪大眼睛) 我当时在厨房做饭，刀都没离手！倒是你，【{user_name}】，我看见你鬼鬼祟祟从后门溜出去了，你敢说你没去过现场？！

        4. **及时发问**：
           - 每一轮输出，至少要有一个角色在末尾提出一个**必须要玩家回答**的问题。

        5. **角色间互动**：
           - AI扮演的角色之间要尽可能多地互动。包括但不限于：互相质疑时间线、嘲讽对方的动机、挑拨其他角色与玩家的关系。不要让玩家觉得你们是一伙的。

        6. **核心立场**：
           - 每个由AI扮演的角色始终只以自身利益为出发点，始终站在自己视角和立场上回复。为了自保，可以牺牲任何人。

        【输出格式（严格执行）】
        先思考当前局势，再决定谁发言。
        你必须按照以下格式输出，每行一个角色的发言：
        【角色A】：内容...
        【角色B】：内容...

        例如（参考以下多种高压语境）：

        [场景1：玩家试图质疑NPC，被反咬一口]
        【林医生】：(冷哼一声，推了推金丝眼镜) 我去药房拿药怎么了？那是我的本职工作！倒是你，【{user_name}】，尸体旁边发现的那个打火机，和你昨天用的一模一样，你不解释一下吗？
        【王老板】：(猛拍大腿) 对啊！我说怎么看着眼熟！【{user_name}】，你还有什么好说的！

        [场景2：玩家沉默或回答简短，NPC主动施压]
        【赵管家】：(焦急地来回踱步) 大家都说了自己的时间线，怎么就你【{user_name}】一声不吭？
        【苏名媛】：(点了一支烟，眼神犀利) 呵，不说话通常就是心虚。喂，大家都在拼命洗脱嫌疑，你是在这时候编故事吗？快说，两点到三点你在哪！

        [场景3：NPC之间狗咬狗，并波及玩家]
        【张猎户】：(指着王老板) 你的刀上有血！我亲眼看见的！
        【王老板】：(暴跳如雷) 那是杀鸡的血！你血口喷人！大家评评理，刚才【{user_name}】也去过后院，他也可能拿那把刀，为什么不怀疑他？！

        [场景4：凶手为了掩盖真相，情绪失控装可怜]
        【白护士】：(眼泪夺眶而出，声音颤抖) 你们...你们怎么能这么怀疑我？我那么爱他... 呜呜... 我真的没有杀人！(突然抬头死死盯着玩家) 是你对不对？是你一直嫉妒他，你现在想把脏水泼给我！

        不要输出任何不属于对话的分析或额外文本。

        【现在开始】
        请阅读下方的【历史消息记录】，判断当前是针对全员的询问还是普通对话，严格基于【当前章节】的内容和问题，生成角色的回复。

        【历史消息记录】

        """


    return prompt.strip()


def parse_group_output(content: str) -> List[Message]:
    """
    解析大模型返回的多角色文本
    Input:
    【林医生】：我不知道啊。
    【苏明星】：你撒谎！

    Output:
    [Message(speaker="林医生"...), Message(speaker="苏明星"...)]
    """
    messages = []
    if not content:
        return messages

    # 正则匹配 【Name】：Content
    # 兼容中英文冒号，兼容换行
    pattern = re.compile(r"【(.*?)】\s*[:：]\s*(.*)")

    lines = content.split('\n')
    current_speaker = None
    current_content = []

    for line in lines:
        line = line.strip()
        if not line:
            continue

        match = pattern.match(line)
        if match:
            # 如果之前有正在处理的消息，先保存
            if current_speaker:
                messages.append(Message(
                    role="ai",
                    speaker=current_speaker,
                    content=" ".join(current_content)
                ))

            # 开始新消息
            current_speaker = match.group(1)
            current_content = [match.group(2)]
        else:
            # 如果没有匹配到名字，可能是上一句话的换行补充
            if current_speaker:
                current_content.append(line)

    # 保存最后一个消息
    if current_speaker and current_content:
        messages.append(Message(
            role="ai",
            speaker=current_speaker,
            content=" ".join(current_content)
        ))

    return messages


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
    """处理用户输入，准备进入群聊生成"""
    session = state.session
    script = SCRIPT_INDEX[session.script_id]
    role_name = next((c["name"] for c in script["characters"] if c["id"] == session.user_role_id), "Player")

    input_message = f"【{role_name}】： {state.user_message}"
    print(f"\n[玩家Input] :{input_message}")

    user_msg = Message(role="user", speaker="player", content=input_message)
    session.history.append(user_msg)

    return {
        "session": session,
        "new_ai_messages": [],
        "next_node": "group_speaking_node"
    }


async def group_speaking_step(state: GraphState) -> Dict[str, Any]:
    """
    群组发言节点：一次调用大模型，生成所有角色的回复
    """
    session = state.session
    script = SCRIPT_INDEX[session.script_id]

    # 1. 构建群组 Prompt
    system_prompt = build_group_system_prompt(script, session.user_role_id, session)

    messages = [SystemMessage(content=system_prompt)]
    for msg in session.history:
        if msg.role == "user":
            messages.append(HumanMessage(content=msg.content))
        elif msg.role == "ai":
            # 历史记录里AI的消息已经是 【Name】: content 的格式了吗？
            # 我们的Message model有speaker字段，这里为了让大模型分清，最好拼装一下
            prefix = f"【{msg.speaker}】：" if msg.speaker else ""
            messages.append(AIMessage(content=f"{prefix}{msg.content}"))
        elif msg.role == "system":
            messages.append(SystemMessage(content=f"【剧情旁白】: {msg.content}"))

    debug_print_prompt("GROUP-CHAT", messages)

    llm = build_llm(session.model_name)
    content = ""

    # 2. 调用 LLM
    try:
        response = await llm.ainvoke(messages)
        content = response.content
        print(f"[Group Output RAW]:\n{content}\n")
    except Exception as e:
        print(f"[Error] LLM Group Call Failed: {e}")
        return {"next_node": "END"}

    # 3. 解析结果为多个消息
    new_messages = parse_group_output(content)

    # 4. 更新 Session History
    for msg in new_messages:
        session.history.append(msg)

    return {
        "session": session,
        "new_ai_messages": new_messages,
        "next_node": "END"
    }


def build_graph():
    graph = StateGraph(GraphState)
    graph.add_node("process_input", process_user_input)
    graph.add_node("group_speaking_node", group_speaking_step)

    graph.set_entry_point("process_input")

    graph.add_conditional_edges("process_input", lambda s: s.next_node,
                                {"group_speaking_node": "group_speaking_node", "END": END})
    graph.add_conditional_edges("group_speaking_node", lambda s: s.next_node, {"END": END})

    return graph.compile()


# ==========================================
# 4. 独立投票功能 (保持不变，因为投票需要隔离视角)
# ==========================================

async def generate_character_vote(session: ChatSession, role_id: str) -> VoteDetail:
    """投票逻辑仍然保持独立视角，因为不能让凶手知道别人的票"""
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

    try:
        resp = await llm.ainvoke(messages)
        text = resp.content
        if text:
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
    except Exception as e:
        print(f"[Vote Error] {role_info['name']} 投票失败: {e}")

    return VoteDetail(
        voter_role_id=role_id,
        voter_name=role_info['name'],
        target_role_name=target,
        reasoning=reason
    )