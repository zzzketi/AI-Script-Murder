package com.ai.aiscriptmurde.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.RoomWarnings;

import com.ai.aiscriptmurde.model.ChatSession;
import java.util.List;

@Dao
public interface ChatDao {

    @Insert
    void insertMessage(ChatMessage message);

    @Query("SELECT * FROM chat_messages WHERE script_id = :scriptId ORDER BY timestamp ASC")
    List<ChatMessage> getHistoryByScriptId(String scriptId);

    @Query("DELETE FROM chat_messages WHERE script_id = :scriptId")
    void clearHistory(String scriptId);

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT c.script_id as scriptId, c.content as lastMessage, c.timestamp as timestamp " +
           "FROM chat_messages c " +
           "INNER JOIN (SELECT script_id, MAX(timestamp) as max_timestamp FROM chat_messages GROUP BY script_id) s " +
           "ON c.script_id = s.script_id AND c.timestamp = s.max_timestamp " +
           "GROUP BY c.script_id " + 
           "ORDER BY c.timestamp DESC")
    List<ChatSession> getAllChatSessions();

    /**
     * 🔥 优化：增加过滤条件，排除所有系统提示和主持人消息
     */
    @Query("SELECT * FROM chat_messages WHERE script_id = :scriptId " +
           "AND (sender_name LIKE :query OR content LIKE :query) " +
           "AND sender_name NOT LIKE '%系统%' " +
           "AND sender_name NOT LIKE '%主持人%' ORDER BY timestamp DESC")
    List<ChatMessage> searchMessages(String scriptId, String query);
}