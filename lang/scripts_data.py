import json
import os
from typing import List, Optional, Dict

from models import ScriptBrief, ScriptDetail

DATA_DIR = os.path.join(os.path.dirname(__file__), "data")

# 简单 mock 剧本数据（你可以替换为从数据库/文件读取）
SCRIPT_LIST_DATA = [
    {
        "id": "001",
        "title": "斑点带子案",
        "desc": "两年前的雷雨夜，姐姐惊恐地喊着“斑点带子”后离奇死亡。现在，妹妹也听到了同样的哨声...",
        "image": "cover_001",
        "score": 9.2,
        "difficulty": "新手",
        "tags": ["经典改编", "密室", "悬疑", "3人"]
    },
    {
        "id": "002",
        "title": "巴德先生的信",
        "desc": "在权力更迭的敏感时刻，巴德将军被发现死在城堡花园中，背插利刃、死状凄惨 ，是谁杀死了他？",
        "image": "cover_002",
        "score": 8.5,
        "difficulty": "新手",
        "tags": ["宫廷", "阵营","新手"]
    },
    {
        "id": "003",
        "title": "长安夜行录",
        "desc": "上元灯节，名妓离奇暴毙，牵扯出朝堂惊天阴谋。",
        "image": "cover_003",
        "score": 9.0,
        "difficulty": "进阶",
        "tags": ["古风", "阵营", "6人"]
    },
    {
        "id": "004",
        "title": "最后的源代码",
        "desc": "就在上市前夜，CTO惨死在封闭的服务器机房，凶手是AI？",
        "image": "cover_004",
        "score": 8.8,
        "difficulty": "烧脑",
        "tags": ["科幻", "黑客", "4人"]
    },
    {
        "id": "005",
        "title": "雪国列车",
        "desc": "列车被积雪困住，一名乘客身中十二刀，凶手就在车厢内。",
        "image": "cover_005",
        "score": 9.5,
        "difficulty": "经典",
        "tags": ["致敬", "本格", "7人"]
    },
    {
        "id": "006",
        "title": "萤火之夏",
        "desc": "那年夏天的约定，随着时间胶囊的开启，揭开了残酷真相。",
        "image": "cover_006",
        "score": 9.6,
        "difficulty": "简单",
        "tags": ["情感", "沉浸", "哭本"]
    },
    {
        "id": "007",
        "title": "仁心医院",
        "desc": "午夜的停尸房传来哭声，消失的尸体究竟去了哪里？",
        "image": "cover_007",
        "score": 8.2,
        "difficulty": "中等",
        "tags": ["惊悚", "变格", "5人"]
    },
    {
        "id": "008",
        "title": "深空失忆",
        "desc": "飞船偏离航线，休眠舱意外开启，谁是潜伏的异形？",
        "image": "cover_008",
        "score": 8.9,
        "difficulty": "困难",
        "tags": ["太空", "生存", "非对称"]
    },
    {
        "id": "009",
        "title": "午夜博物馆",
        "desc": "镇馆之宝失窃，安保系统毫无反应，大盗就在我们中间。",
        "image": "cover_009",
        "score": 7.8,
        "difficulty": "欢乐",
        "tags": ["欢乐", "机制", "4人"]
    }
]


def get_script_detail(script_id: str) -> Optional[ScriptDetail]:
    """
  根据 ID 读取 data 目录下的 JSON 文件
  """
    filename = f"script_{script_id}.json"
    file_path = os.path.join(DATA_DIR, filename)

    if not os.path.exists(file_path):
        return None

    try:
        with open(file_path, "r", encoding="utf-8") as f:
            data = json.load(f)
        # 将 JSON 转换为 Pydantic 模型
        return ScriptDetail(**data)
    except Exception as e:
        print(f"Error loading script {script_id}: {e}")
        return None

