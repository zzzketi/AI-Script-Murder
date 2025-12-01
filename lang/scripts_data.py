from typing import List, Dict

# 简单 mock 剧本数据（你可以替换为从数据库/文件读取）
SCRIPTS = [
    {
        "id": "script_1",
        "title": "午夜列车谋杀案",
        "cover_url": "https://example.com/covers/train.jpg",
        "description": "深夜从A市开往B市的列车上，一起离奇命案……",
        "background": (
            "故事发生在一列行驶在深夜中的特快列车上。"
            "列车经过一段无人区时突然停电，灯光熄灭。"
            "再次亮起时，一位乘客被发现死在自己的包厢内。"
            "所有人都声称自己没有离开过座位……"
        ),
        "author": {
            "id": "author_1",
            "name": "剧本作者 · 小李",
            "avatar": "https://example.com/authors/xiaoli.png",
            "bio": "专注现代推理本创作的独立作者。"
        },
        "characters": [
            {
                "id": "c1",
                "name": "你（玩家可选任意一位）",
                "short_desc": "请在实际选择时选具体角色，避免使用这个占位角色。",
                "detail": "占位角色，不建议在 UI 中直接展示。"
            },
            {
                "id": "detective",
                "name": "刑警·周明",
                "short_desc": "冷静理智的刑警，负责接手本案侦查。",
                "detail": "33岁，办案经验丰富，习惯从细节中寻找真相。"
            },
            {
                "id": "doctor",
                "name": "乘客·林医生",
                "short_desc": "外科医生，自称案发时一直在车厢里看病例。",
                "detail": "40岁，行医多年，性格温和，但似乎隐瞒了什么。"
            },
            {
                "id": "student",
                "name": "乘客·大学生小王",
                "short_desc": "理工科学生，喜欢推理小说。",
                "detail": "21岁，口碑不错的学霸，案发时声称在走廊打电话。"
            },
            {
                "id": "attendant",
                "name": "列车员·张倩",
                "short_desc": "负责本车厢的列车员。",
                "detail": "28岁，对列车结构、停靠站点非常熟悉。"
            },
        ],
    },
    {
        "id": "script_2",
        "title": "古堡遗嘱之夜",
        "cover_url": "https://example.com/covers/castle.jpg",
        "description": "富豪离世前夜，亲属被召集到古堡宣读遗嘱，一夜之间风云突变。",
        "background": "偏远山顶古堡，暴风雨之夜，遗产纠纷与旧日恩怨交织……",
        "author": {
            "id": "author_2",
            "name": "剧本作者 · 七号",
            "avatar": "https://example.com/authors/seven.png",
            "bio": "偏爱哥特与悬疑氛围的剧本作者。"
        },
        "characters": [
            {
                "id": "lawyer",
                "name": "律师·陈晨",
                "short_desc": "受托宣读遗嘱的律师。",
                "detail": "稳重谨慎，对富豪家族秘辛略知一二。"
            },
            {
                "id": "heiress",
                "name": "长女·苏婉",
                "short_desc": "富豪长女，强势干练。",
                "detail": "在公司管理中发挥核心作用，对遗产分配格外敏感。"
            },
        ],
    },

    # ---------------- 新增剧本 3：都市密室推理 ----------------
    {
        "id": "script_3",
        "title": "封闭公寓的最后一夜",
        "cover_url": "https://example.com/covers/apartment.jpg",
        "description": "一栋即将被整体拆除的老式公寓中，发生了离奇密室死亡事件。",
        "background": (
            "老城区的一栋公寓，因为城市改造计划将于次日清晨正式拆除。"
            "当晚，几位曾经的老住户受邀回来参加“告别派对”。"
            "午夜时分，忽然有一位住户被发现死在反锁的房间内，"
            "门窗从内部反锁，没有打斗痕迹，监控却在关键三分钟全部黑屏……"
        ),
        "author": {
            "id": "author_3",
            "name": "剧本作者 · 白栀",
            "avatar": "https://example.com/authors/baizhi.png",
            "bio": "擅长都市现实题材与细节流推理的作者。"
        },
        "characters": [
            {
                "id": "old_owner",
                "name": "原房东 · 许大海",
                "short_desc": "曾经的整栋公寓房东，如今经营着多处房产。",
                "detail": "56岁，性格爽朗，早年靠出租房起家，如今事业有成，但与旧租客之间似乎还有未了的账。"
            },
            {
                "id": "designer",
                "name": "青年设计师 · 顾北",
                "short_desc": "回到公寓取走曾经的设计手稿。",
                "detail": "28岁，个性安静，学生时代曾在此长住，对老楼的结构细节非常熟悉。"
            },
            {
                "id": "blogger",
                "name": "城市探访博主 · 林杉",
                "short_desc": "为拍摄“城市消失空间”专题而来。",
                "detail": "24岁，新晋自媒体人，对一切都好奇心爆棚，却也擅长剪辑和“制造话题”。"
            },
            {
                "id": "caretaker",
                "name": "老看门人 · 王婶",
                "short_desc": "看门三十年，守着这栋公寓的一切秘密。",
                "detail": "62岁，嘴上唠叨但心里细腻，似乎知道很多住户都不想被揭开的过去。"
            },
        ],
    },

    # ---------------- 新增剧本 4：古风权谋本 ----------------
    {
        "id": "script_4",
        "title": "长安月下的密诏",
        "cover_url": "https://example.com/covers/changan_moon.jpg",
        "description": "大宴之前，密诏失窃；大宴之后，尸体横陈。",
        "background": (
            "大晟王朝，天元十七年，中秋前夜。"
            "长安城内，丞相府设宴，文武百官齐聚一堂。"
            "盛宴之中，有传言称朝中将有大换血，而关键的一道密诏，却在当夜离奇失踪。"
            "次日清晨，丞相密室中却多出了一具诡异的尸体……"
        ),
        "author": {
            "id": "author_4",
            "name": "剧本作者 · 青瓷",
            "avatar": "https://example.com/authors/qingci.png",
            "bio": "专注古风权谋与情感纠葛的作者，擅长多线索叙事。"
        },
        "characters": [
            {
                "id": "prime_minister",
                "name": "丞相 · 苏谨言",
                "short_desc": "位极人臣，深受圣上倚重。",
                "detail": "四十出头，行事缜密，喜怒不形于色，传言其手中握有诸多权贵的把柄。"
            },
            {
                "id": "princess",
                "name": "长公主 · 赵清莹",
                "short_desc": "皇族中地位特殊的存在。",
                "detail": "风姿绰约，却不甘于宫闱之中，私下与多方势力有所牵连。"
            },
            {
                "id": "guard_leader",
                "name": "禁军统领 · 贺仲",
                "short_desc": "统领皇城禁军，负责当夜安保。",
                "detail": "武艺高强，出身寒门，对苏府与皇权之间的微妙平衡极为敏感。"
            },
            {
                "id": "scribe",
                "name": "书吏 · 卢之远",
                "short_desc": "负责誊抄密诏的书吏。",
                "detail": "文弱书生模样，却经手无数机密文件，是少数真正目睹过密诏内容之人。"
            },
        ],
    },

    # ---------------- 新增剧本 5：校园青春本（轻悬疑） ----------------
    {
        "id": "script_5",
        "title": "毕业照背后的秘密",
        "cover_url": "https://example.com/covers/graduation.jpg",
        "description": "一张被意外翻出的老毕业照，让所有人陷入了那年夏天的回忆。",
        "background": (
            "某普通高中，十年一度的校友返校日。"
            "一位当年班主任忽然离奇失踪，而在整理旧物时，"
            "几位老同学发现了一张“从未公开过”的合影——"
            "照片中多出的那个人，谁也记不起他是谁。"
        ),
        "author": {
            "id": "author_5",
            "name": "剧本作者 · 番茄鱼",
            "avatar": "https://example.com/authors/fqy.png",
            "bio": "喜欢把日常与温情包裹在悬疑外壳里的作者。"
        },
        "characters": [
            {
                "id": "monitor",
                "name": "前班长 · 赵一帆",
                "short_desc": "曾经的“全能学生”，如今是职场新人。",
                "detail": "28岁，在大城市打拼，回母校更多是为了重新确认自己。"
            },
            {
                "id": "problem_kid",
                "name": "昔日“问题学生” · 孙默",
                "short_desc": "那年最让老师头疼的学生。",
                "detail": "当年因一次事故提前退学，多年后再相见，性格似乎大变。"
            },
            {
                "id": "goddess",
                "name": "班花 · 李安然",
                "short_desc": "当年所有人口中的“女神”。",
                "detail": "毕业后远赴他国求学，这次返校像是蓄谋已久。"
            },
            {
                "id": "photographer",
                "name": "摄影社学长 · 周临",
                "short_desc": "神秘的“多出来的那个人”。",
                "detail": "老照片的主角之一，十年前的真相似乎与他密切相关。"
            },
        ],
    },

    # ---------------- 新增剧本 6：科幻太空本 ----------------
    {
        "id": "script_6",
        "title": "逐光号失联记录",
        "cover_url": "https://example.com/covers/spaceship.jpg",
        "description": "人类首艘深空殖民船“逐光号”，在飞行第十年突然与地球失去联络。",
        "background": (
            "公元2189年，人类发射“逐光号”深空殖民船，"
            "搭载数百名精英与冷冻乘客，前往比邻星系。"
            "飞行第十年的例行状态汇报中，逐光号的日志出现严重错乱，"
            "随后，所有信号中断。"
            "多年后，地球终于截获了一段来自逐光号的“延迟通讯片段”……"
        ),
        "author": {
            "id": "author_6",
            "name": "剧本作者 · 零度",
            "avatar": "https://example.com/authors/zero.png",
            "bio": "专注科幻与哲学向剧本，喜欢讨论“人性在极端环境下”的变化。"
        },
        "characters": [
            {
                "id": "captain",
                "name": "舰长 · 艾伦",
                "short_desc": "逐光号的最高指挥官。",
                "detail": "40岁，服役背景深厚，在多次太空任务中表现优异，对“任务优先”有近乎偏执的坚持。"
            },
            {
                "id": "ai_officer",
                "name": "AI维护官 · 许嘉",
                "short_desc": "负责维护舰上主控AI“露娜”的工程师。",
                "detail": "32岁，性格理性冷静，坚信科技是解决一切问题的钥匙。"
            },
            {
                "id": "psychologist",
                "name": "心理学家 · 莉亚",
                "short_desc": "为了长期航行而配备的心理专家。",
                "detail": "35岁，善于倾听，却似乎在个人档案中隐藏了某些关键经历。"
            },
            {
                "id": "frozen_colonist",
                "name": "冷冻乘客代表 · 诺亚",
                "short_desc": "从冷冻仓中提前被唤醒的殖民志愿者。",
                "detail": "本应在抵达目标星系前保持冷冻状态，却因一次“事故”被提前唤醒，对当前局面一无所知。"
            },
        ],
    },

    # ---------------- 新增剧本 7：现代社交推理本 ----------------
    {
        "id": "script_7",
        "title": "消失的群聊记录",
        "cover_url": "https://example.com/covers/chatgroup.jpg",
        "description": "一个熟人局的微信群，在某人意外身亡后，聊天记录被人“精心”删除过。",
        "background": (
            "六位大学同学毕业多年后依旧保持着一个小群，"
            "平日里互相吐槽生活、分享八卦。"
            "某天，其中一人突然坠楼身亡，警方判定为自杀。"
            "然而，在他的手机中，这个群的部分聊天记录被人彻底清空了……"
        ),
        "author": {
            "id": "author_3",  # 复用作者3：白栀
            "name": "剧本作者 · 白栀",
            "avatar": "https://example.com/authors/baizhi.png",
            "bio": "擅长都市现实题材与细节流推理的作者。"
        },
        "characters": [
            {
                "id": "group_owner",
                "name": "群主 · 韩啸",
                "short_desc": "看似活泼的“气氛担当”。",
                "detail": "社交能力极强，习惯调和矛盾，却在死者出事前后沉默异常。"
            },
            {
                "id": "top_student",
                "name": "学霸 · 宋言",
                "short_desc": "从学生时代就一路顺风顺水。",
                "detail": "如今在大厂工作，看上去人生赢家，但他比任何人都害怕过去被翻出来。"
            },
            {
                "id": "roommate",
                "name": "前室友 · 郑池",
                "short_desc": "与死者同住过四年的人。",
                "detail": "对死者的生活习惯了如指掌，却对案发当天的细节三缄其口。"
            },
            {
                "id": "gamer",
                "name": "游戏主播 · 梁柚",
                "short_desc": "靠直播为生的自由职业者。",
                "detail": "平时熬夜打游戏，案发时间正好在直播，但她的直播回放却被部分删除。"
            },
        ],
    },

    # ---------------- 新增剧本 8：古风奇谭本 ----------------
    {
        "id": "script_8",
        "title": "十里桃林的最后一盏灯",
        "cover_url": "https://example.com/covers/peachforest.jpg",
        "description": "村中相传，每当桃花尽落，最后一盏灯灭时，便会有人离开这个世界。",
        "background": (
            "偏僻山村，桃林环绕，每年花季都吸引画师与游人前来。"
            "村口有一座古老的灯楼，据说点燃的是“镇魂灯”。"
            "某年春末，镇魂灯忽明忽暗，村里陆续发生诡异事件，"
            "而你们，都与十年前的一场“失踪案”有关。"
        ),
        "author": {
            "id": "author_4",  # 复用作者4：青瓷
            "name": "剧本作者 · 青瓷",
            "avatar": "https://example.com/authors/qingci.png",
            "bio": "专注古风权谋与情感纠葛的作者，擅长多线索叙事。"
        },
        "characters": [
            {
                "id": "village_head",
                "name": "村长 · 梁大河",
                "short_desc": "一辈子没离开过这座山村。",
                "detail": "年近六十，见证了村中太多怪事，对灯楼的秘密讳莫如深。"
            },
            {
                "id": "painter",
                "name": "游走画师 · 沈笙",
                "short_desc": "专门收集“离奇传说”的画师。",
                "detail": "外乡人，自称是为了取材而来，却对十年前的失踪案细节异常了解。"
            },
            {
                "id": "herbalist",
                "name": "采药人 · 阿芷",
                "short_desc": "常年出入深山采药。",
                "detail": "性格寡言，不太与人亲近，似乎在躲避着什么。"
            },
            {
                "id": "orphan",
                "name": "孤儿 · 小驹",
                "short_desc": "在村里长大的孤儿。",
                "detail": "对灯楼和桃林都有莫名熟悉的感觉，总能找到别人找不到的小路。"
            },
        ],
    },
]

# 为了搜索演示做一个简单索引
SCRIPT_INDEX: Dict[str, dict] = {s["id"]: s for s in SCRIPTS}

AUTHOR_INDEX: Dict[str, dict] = {}
for s in SCRIPTS:
    author = s["author"]
    if author["id"] not in AUTHOR_INDEX:
        AUTHOR_INDEX[author["id"]] = {
            "id": author["id"],
            "name": author["name"],
            "avatar": author["avatar"],
            "bio": author["bio"],
            "scripts": [],
        }
    AUTHOR_INDEX[author["id"]]["scripts"].append(
        {
            "id": s["id"],
            "title": s["title"],
            "cover_url": s["cover_url"],
            "description":s["description"],
        }
    )