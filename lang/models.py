# models.py
from typing import List, Literal, Dict, Optional
from pydantic import BaseModel, Field
from datetime import datetime


class Character(BaseModel):
    id: str
    name: str
    short_desc: str
    detail: str


class Author(BaseModel):
    id: str
    name: str
    avatar: str
    bio: str


class ScriptBrief(BaseModel):
    id: str
    title: str
    cover_url: str
    description: str


class ScriptDetail(BaseModel):
    id: str
    title: str
    cover_url: str
    description: str
    background: str
    author: Author
    characters: List[Character]


class Message(BaseModel):
    role: Literal["user", "ai", "system"]
    speaker: Optional[str] = None  # 说话的角色id / 名称
    content: str
    timestamp: datetime = Field(default_factory=datetime.utcnow)


class ChatSession(BaseModel):
    session_id: str
    script_id: str
    user_role_id: str
    model_name: str
    history: List[Message] = Field(default_factory=list)


# 简单的「内存」会话存储，实际可换成数据库/Redis
class InMemorySessionStore:
    def __init__(self):
        self._sessions: Dict[str, ChatSession] = {}

    def create_session(
        self, session_id: str, script_id: str, user_role_id: str, model_name: str
    ) -> ChatSession:
        session = ChatSession(
            session_id=session_id,
            script_id=script_id,
            user_role_id=user_role_id,
            model_name=model_name,
            history=[],
        )
        self._sessions[session_id] = session
        return session

    def get_session(self, session_id: str) -> Optional[ChatSession]:
        return self._sessions.get(session_id)

    def update_session(self, session: ChatSession):
        self._sessions[session.session_id] = session

    def clear_history(self, session_id: str):
        if session_id in self._sessions:
            self._sessions[session_id].history = []

    def list_sessions(self) -> List[ChatSession]:
        # 简单返回全部；你可以根据用户id过滤
        return list(self._sessions.values())

    def delete_session(self, session_id: str):
        self._sessions.pop(session_id, None)