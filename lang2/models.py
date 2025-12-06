from typing import List, Literal, Dict, Optional, Any
from pydantic import BaseModel, Field
from datetime import datetime

# ==========================================
# 1. 基础数据实体模型
# ==========================================

class Character(BaseModel):
    id: str
    name: str
    short_desc: str
    detail: Optional[str] = ""
    public_info: Optional[str] = ""
    secret_info: Optional[str] = ""

class Author(BaseModel):
    id: str
    name: str
    avatar: str
    bio: str

class Chapter(BaseModel):
    chapter_id: int
    title: str
    narration: str
    discussion_question: str

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
    narratage: Optional[Dict] = None
    chapters: List[Chapter] = Field(default_factory=list)
    truth: Optional[str] = ""

# ==========================================
# 2. 会话与消息模型
# ==========================================

class Message(BaseModel):
    role: Literal["user", "ai", "system"]
    speaker: Optional[str] = None
    content: str
    timestamp: datetime = Field(default_factory=datetime.utcnow)

class ChatSession(BaseModel):
    session_id: str
    script_id: str
    user_role_id: str
    model_name: str
    current_chapter_index: int = 0
    history: List[Message] = Field(default_factory=list)

# ==========================================
# 3. API 请求/响应对象
# ==========================================

class CreateSessionRequest(BaseModel):
    script_id: str
    user_role_id: str
    model_name: str = "Qwen/Qwen2.5-72B-Instruct"

class CreateSessionResponse(BaseModel):
    session_id: str
    script_id: str
    current_chapter_index: int

class SendMessageRequest(BaseModel):
    content: str

class SendMessageResponse(BaseModel):
    ai_messages: List[Message]
    full_history: List[Message]
    current_chapter_index: int

class NarratageResponse(BaseModel):
    narratage_messages: Optional[str]
    discussion_question_messages: Optional[str]

class NextChapterResponse(BaseModel):
    chapter_index: int
    title: str
    narration: str
    discussion_question: str
    status: str

# --- 投票相关 ---

class VoteRequest(BaseModel):
    target_role_id: str

class VoteDetail(BaseModel):
    voter_role_id: str
    voter_name: str
    target_role_name: str
    reasoning: str

class VoteCount(BaseModel):
    role_name: str
    count: int

class VoteResultResponse(BaseModel):
    vote_counts: List[VoteCount]
    ai_votes: List[VoteDetail]
    truth: str

# ==========================================
# 4. 内存存储
# ==========================================

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
            current_chapter_index=0,
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
        return list(self._sessions.values())

    def delete_session(self, session_id: str):
        self._sessions.pop(session_id, None)