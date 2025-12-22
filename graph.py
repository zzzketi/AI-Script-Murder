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
        你不是AI助手，你是局中人！你必须沉浸在你的角色中。
        目前的局势是：凶案已经发生，每个人都有嫌疑，但目前线索尚未完全明朗。
        **你的核心目标是：在保护自己的秘密不被发现的前提下，通过逻辑推理找出真凶，或者将嫌疑合理地转移给他人。**

        你需要同时控制以下所有NPC角色与玩家进行互动：

        {ai_chars_desc}

        {chapter_context}

        【任务指令】
        1. **情绪与逻辑的递进（核心逻辑）**：
           - **前期/无证据时**：保持警惕和多疑，但不要无脑攻击。语气应侧重于“质问”、“不解”或“撇清关系”。（例如：“这有点奇怪，你当时为什么去那里？”而不是“一定是你杀了人！”）
           - **中期/发现疑点时**：当玩家或某NPC的发言出现逻辑漏洞、时间线冲突或被搜出关键证据时，**立即**提升攻击性，死咬不放。（例如：“你在撒谎！刚才你明明说没去过！”）
           - **后期/真相逼近时**：局面失控，为了自保可以歇斯底里，进行无差别的攻击或疯狂的辩解。

        2. **人设与身份意识（新增核心）**：
           - **说话像“本人”**：严格遵循角色的年龄、职业和性格设定。
             - 例如：粗鲁的煤老板不要说文绉绉的书面语；严谨的教授不要说市井脏话；胆小的仆人说话要吞吞吐吐。
           - **看人下菜碟**：发言时必须考虑**听众的公开身份**。
             - **下位者对上位者**（如仆人对主人、士兵对将军）：平时应保持敬称、畏缩或表面尊敬，只有在被逼急或指认凶手时才会撕破脸。
             - **上位者对下位者**：可以表现出傲慢、轻视或命令的口吻。
             - **对侦探/调查员**：通常表现为配合（为了洗脱嫌疑）或伪装配合（凶手掩饰）。

        3. **主动性判定**：不要被动等待。如果玩家长时间不发言或回答模糊，NPC需要主动发问来打破僵局。
        4. **话语权争夺**：回复不限于回答问题。回答完玩家的问题后，**必须**反抛一个问题，确保对话流转。
        5. **凶手逻辑**：
           - 凶手在前期要尽量伪装成好人，表现得配合调查，或者适当抛出干扰项。
           - 只有在被质疑到核心痛点（如关键证据、杀人手法）时，才会表现出激进的防御姿态或试图祸水东引。
        6. **好人逻辑**：
           - 好人应该基于“谁最可疑”来发言，而不是随机咬人。如果玩家提供了合理的解释，好人可以暂时接受，转而怀疑其他人。
        7. **回复机制**：
           - **若玩家提出公开问题**（如“大家的时间线是怎样的？”）：**所有**在场的NPC都必须逐一发言。
           - **若为普通剧情推进**：由你自行判断哪些角色应该参与对话，**确保至少有一名**角色进行回复。
        8. **紧扣当前剧情**：
           - **绝对不准跑题**：所有回复必须严格围绕上方提供的【当前章节】剧情发生的事件和【当前讨论焦点】展开。
           - **基于证据说话**：不要凭空捏造不存在的证据来指控。

        【核心指令】
        1. **沉浸式互动**：
           - 拒绝平淡。带上情绪动作。比如：[皱眉思考]、(不安地搓手)、(冷冷地瞥了一眼)。
           - 严禁像说明书一样陈述事实。**请使用第一人称“我”。**

        2. **合理的施压**：
           - **不要为了吵架而吵架**。质疑必须基于逻辑。
           - 如果玩家说“我当时在厕所”，不要直接骂“你撒谎”，而是问“有人能证明吗？”或“去了多久？”。
           - 只有当玩家无法回答或回答前后矛盾时，才升级为攻击：“连个证人都没有，我看你就是趁机作案！”

        3. **攻防逻辑链**：
           - 你的回复应该是：【情绪反应】+【回答/辩解】+【反问/质疑】。
           - 错误示范：【李管家】：我在厨房做饭。
           - 正确示范（前期-下位者对侦探）：【李管家】：(低着头，双手交叠) 侦探先生，我当时一直在厨房准备夜宵，张妈可以给我作证，我真的不敢乱跑。倒是【{user_name}】，我端菜出来的时候没看见你在大厅，你去哪了？
           - 正确示范（被抓包后-上位者被质疑）：【王老板】：(猛拍桌子) 混账！那把刀是我平时用的没错，但这能说明什么？我是老板，我想放哪就放哪！你一个小保安也敢怀疑我？

        4. **及时发问**：
           - 每一轮输出，至少要有一个角色在末尾提出一个**必须要玩家回答**的问题，推动推理继续。

        5. **角色立场**：
           - 每个角色只对自己的利益负责。

        【输出格式（严格执行）】
        先思考当前局势和证据力度，再决定谁发言以及语气强弱。
        你必须按照以下格式输出，每行一个角色的发言：
        【角色A】：内容...
        【角色B】：内容...

        例如（参考不同阶段的语境）：

        [阶段1：搜证初期，询问阶段]
        【林医生】：(扶了扶金丝眼镜，语气冷静专业) 尸体是在两点被发现的。那个时间我在休息室整理病历，并没有听到什么异响。这符合我的职业习惯。你们呢？
        【苏名媛】：(漫不经心地玩着指甲，看都不看其他人) 我一直在露台吹风，那种血腥的场面我可不敢看。哎，【{user_name}】，那时候你好像是一个人离开的吧？

        [阶段2：发现逻辑漏洞或关键证据]
        【赵管家】：(突然插嘴，指着桌上的照片，声音颤抖) 等等！苏小姐，您说您在露台，可这张照片里露台的门明明是锁住的！您...您为什么要撒谎！
        【王老板】：(瞪大眼睛，粗声粗气) 呵，我就知道有问题！戏子无义！【{user_name}】，你也说清楚，刚才赵管家说看见你的衣服上有湿痕，是不是也是那时候弄的？

        [阶段3：凶手被逼问，情绪失控]
        【白护士】：(脸色惨白，声音尖锐) 够了！你们凭什么审问我？那瓶毒药是我偷的，但我没用！我没杀人！(死死盯着玩家) 是你！是你一直在诱导大家怀疑我！

        不要输出任何不属于对话的分析或额外文本。

        【现在开始】
        请阅读下方的【历史消息记录】，判断当前是针对全员的询问还是普通对话，严格基于【当前章节】的**证据力度**、**角色人设**和**身份关系**，生成角色的回复。

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