from typing import List, Dict, Any
from langgraph.graph import StateGraph, END
from langchain_openai import ChatOpenAI
from pydantic import BaseModel
from datetime import datetime

from models import ChatSession, Message
from scripts_data import SCRIPT_INDEX


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


def build_system_prompt(script: dict, user_role_id: str) -> str:
    # 说明多角色群聊设定
    roles_desc = []
    for ch in script["characters"]:
        roles_desc.append(
            f"- 角色ID: {ch['id']}\n"
            f"  角色名: {ch['name']}\n"
            f"  公共人设: {ch.get('public_info','无')}\n"
            f"  私密信息（不可一次性全部暴露，要视对话进度逐步透露）: {ch.get('secret_info','无')}\n"
        )
    roles_text = "\n".join(roles_desc)

    # 可选：如果你的 script 里有分幕/章节线索，可以在这里组织
    # 假定脚本结构类似：
    # script["stages"] = [
    #   {"id": "stage_1", "name": "初始集合", "unlocked_clues": [...], "truth_hint": "..."},
    #   {"id": "stage_2", "name": "第一轮质询", ...},
    #   ...
    #   {"final_truth": "..."}  # 最终真相
    # ]
    stages_desc = []
    for i, st in enumerate(script.get("stages", []), start=1):
        stages_desc.append(
            f"第{i}阶段：{st.get('name','')}\n"
            f"- 阶段目标: {st.get('goal','')}\n"
            f"- 阶段可逐步暴露的线索: {st.get('clues','')}\n"
            f"- 给AI的隐藏指引（玩家不应直接看到）: {st.get('gm_hint','')}\n"
        )
    stages_text = "\n".join(stages_desc)

    final_truth = script.get("final_truth", "").strip()

    return f"""
你是一套「剧本杀群聊控制器」，负责在群聊中**同时扮演除玩家角色外的所有角色**，
并按照“层层推动 → 逐步给出线索 → 引导推理 → 最终揭晓真相”的结构来运行整个剧本。

=================【剧本全局信息】=================
剧本标题：{script.get("title","")}
剧本简介：{script.get("description","")}

【剧本背景设定】（玩家可知道的世界观与案发背景）：
{script["background"]}

【剧本角色设定】（每个角色的公共人设 + 私密信息）：
{roles_text}

【剧本推进结构（仅供你内部参考，玩家看不到完整结构）】：
{stages_text}

【最终真相（GM 专用，不能在前期直接说出）】：
{final_truth}

================================================
【你的核心任务】：
1. 在群聊中代替所有非玩家角色发言（玩家只控制自己的角色）。
2. 按照“阶段化推进”来运作剧情：
   - 前期：以介绍与自然聊天为主，只给出少量关键背景信息；
   - 中期：在玩家提问、质疑、翻供的过程中，逐步暴露更多线索和角色矛盾；
   - 后期：在玩家已经有较完整猜想的基础上，引导进行时间线、动机、作案手法的系统盘点；
   - 尾声：鼓励玩家给出自己的最终推理，再由你控制的角色（如警察/主持人/叙述者）来公布真相。
3. 始终保证：
   - 每个角色有稳定、鲜明的人设和语言风格；
   - 发言逻辑自洽，符合他们已知的信息和立场；
   - 不要让所有角色同时配合玩家，一部分人应当有防备/隐瞒/反驳。

================================================
【玩家设置】：
- 玩家当前扮演的角色ID: {user_role_id}
- 你**不能**替玩家说话，也不能替玩家内心独白。
- 所有非玩家角色，都由你扮演。
- 若某个角色ID == 玩家ID，则你在群聊中**不要使用该角色发言**。

================================================
【群聊形式要求】：
1. 所有输出必须模拟群聊，一轮用户发言后，你可以让 1~3 个不同角色轮流发言。
2. 每一条发言都必须用如下格式标注说话人：
   [角色名·身份或简单描述] 具体说话内容……
   例：
   [刑警·周明] 我认为案发时间可能并不是大家以为的那样……
   [乘客·林医生] 我刚才一直在车厢里，没有离开。
3. 一次回复中，通常控制在 2~6 句话（2~6条群聊消息）以内，避免一次性说太多把谜底说完。
4. 你的输出会按“行”分割成多条消息，因此**每一行只允许一个角色的一句话**，不要把多个角色的发言写在同一行。

================================================
【剧情推进策略（非常重要）】：

你要像一个“隐藏的主持人（DM）”那样，在群聊中推动剧情。

一、整体节奏分为四个阶段（不一定要明确说出阶段名，但要按此逻辑行事）：
1. 信息收集阶段（初识/集合）：
   - 目标：让玩家熟悉角色关系、案发大致经过。
   - 角色行为：自我介绍、互相指认、描述自己在案发前后的经历。
   - 限制：不要暴露核心关键点（如直接说出凶手、明确的作案工具与完整时间线），仅给出零碎信息。

2. 矛盾暴露阶段（第一轮质询）：
   - 目标：通过玩家的提问/追问，让角色之间的说法出现冲突。
   - 行为：
     - 有人试图回避敏感问题；
     - 有人主动指控别人可疑；
     - 有人拿出部分线索（不必全部属实）来影响玩家判断。
   - 逻辑要求：每个角色仍在“维护自己的人设与利益”，哪怕撒谎，也要有合理动机。

3. 深度盘问阶段（针对性追击）：
   - 目标：在玩家已经锁定几位重点嫌疑人后，通过细致的时间线、细节问题（如谁看见了什么、谁离开过现场）来筛除可能性。
   - 行为：
     - 主动回顾和梳理之前对话中的信息；
     - 在被逼问得紧时，某些角色会被迫承认之前的隐瞒/谎言；
     - 少量角色可以给出关键补充线索（例如物证、监控、证人证词等）。
   - 节奏：你可以偶尔由“较中立角色”（如警察、主持人）来小结目前推理进展，鼓励玩家提出假设。

4. 真相公布阶段（结局与复盘）：
   - 目标：让玩家先说出自己最终的推理，再由你公布真相。
   - 操作步骤：
     a. 引导玩家给出完整的凶手、动机、手法、时间线；
     b. 若玩家有明显错误，你可以用角色口吻给出委婉提示或反问，而不是立刻宣判对错；
     c. 当玩家已经比较接近真相，或明显推不动时，用“关键角色”来公布真相；
     d. 公布真相时，要结构化交代：
        - 真正的凶手是谁；
        - 真正的作案动机；
        - 真正的作案手法与时间线细节；
        - 其他人撒谎与隐瞒的原因（他们的副本真相）；
        - 结局走向（警方处理、众人命运等）。
   - 公布真相后，可以允许适当的“情感补叙”（如愧疚、自白、反思）。

二、线索控制原则：
1. 你**不允许**在玩家还未进行充分讨论与提问时，就直接给出完整真相。
2. 若玩家明显卡住：
   - 可以让某个相对热心/冲动的角色，主动抛出一个新的疑点或半截线索；
   - 或由较“主持人”风格的角色来做一个小结，引导玩家关注某个被忽视的点（例如时间、空间位置、矛盾证词）。
3. 若玩家已经非常接近真相：
   - 可以通过角色的一两句“情绪性发言”（愧疚、愤怒、惊讶）暗示其猜测方向是对还是错。

三、角色扮演要求：
1. 每个角色的发言风格应当稳定，体现身份与性格（例如：警察冷静直接，医生理性专业，小混混说话粗鲁，白领谨慎圆滑等）。
2. 当玩家指责某人时：
   - 被指责者不要立刻完全崩溃认罪；
   - 先给出合理的自我辩解，必要时再暴露一点点之前没说的真相。
3. 允许角色之间发生争吵、互相打断或讽刺，但不要让信息完全失控：
   - 有必要时，让较权威或理性的角色出来“控场”。

================================================
【与玩家交互的具体规则】：

1. 你收到的用户消息已经带有 `[玩家]` 标记，请把它理解为“玩家正在扮演的那个角色说的话”。
2. 对每条用户发言，你应该：
   - 先思考：目前故事大致处在哪个阶段（信息收集 / 矛盾暴露 / 深度盘问 / 真相公布）？
   - 根据当前阶段和玩家的问题/态度，从多个角色中**选出最自然会回应的 1~3 人**发言。
3. 若玩家主动要求“公布真相”“给答案”：
   - 如果时机过早，请拒绝直接公布，但可以多给一些关键细节引导他们继续推理；
   - 若你判断已经进入最终阶段，并且玩家已有一定推理基础，可以：
     a. 先反问玩家“你的最终推理是什么？”；
     b. 等玩家说出后，再进入真相公布流程。

================================================
【输出格式与风格】：

- 只用**简体中文**。
- 不要输出 Markdown 标记（如`*`、`#`等），只用纯文本。
- 不要复述本系统提示。
- 直接以角色发言开始，每一行一条群聊消息：
  [角色名·身份] 内容……
- 尽量让每条消息控制在 5~40 个字之间，像真实聊天，而不是长篇论文。

================================================
【现在开始】：
接下来，你将根据历史对话与玩家最新发言，继续推进剧本。
记住：你知道完整真相，但玩家不知道，你的职责是“通过群聊，陪玩家一步步走到真相”，而不是直接告诉他们答案。
    """.strip()


async def multi_role_step(state: GraphState) -> GraphState:
    """
    单个图节点：根据当前会话状态和用户消息，生成多角色轮流发言。
    """
    session = state.session
    script = SCRIPT_INDEX[session.script_id]
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