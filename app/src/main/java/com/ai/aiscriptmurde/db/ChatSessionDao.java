package com.ai.aiscriptmurde.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ChatSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplaceSession(ChatSessionEntity session);

    @Query("SELECT * FROM chat_sessions WHERE scriptId = :scriptId")
    ChatSessionEntity getSessionById(String scriptId);

    @Query("SELECT * FROM chat_sessions ORDER BY timestamp DESC")
    LiveData<List<ChatSessionEntity>> getAllSessions();

    @Update
    void updateSession(ChatSessionEntity session);

    @Query("DELETE FROM chat_sessions WHERE scriptId = :scriptId")
    void deleteSessionById(String scriptId);

    @Query("UPDATE chat_sessions SET lastMessage = :lastMessage, timestamp = :timestamp WHERE scriptId = :scriptId")
    void updateSessionSummary(String scriptId, String lastMessage, long timestamp);

    /**
     * 🔥 新增：将指定会话的未读数 +1
     */
    @Query("UPDATE chat_sessions SET unreadCount = unreadCount + 1 WHERE scriptId = :scriptId")
    void incrementUnreadCount(String scriptId);

    /**
     * 🔥 新增：将指定会话的未读数清零
     */
    @Query("UPDATE chat_sessions SET unreadCount = 0 WHERE scriptId = :scriptId")
    void clearUnreadCount(String scriptId);
}