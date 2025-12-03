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

    /**
     * 优化后的查询：准确地获取每个 script_id 分组中，时间戳最大的那条记录。
     * 1. 内层子查询 `(SELECT script_id, MAX(timestamp) as max_timestamp FROM chat_messages GROUP BY script_id)`
     *    找到每个剧本的最新时间戳。
     * 2. 将原始表 `chat_messages` 与子查询的结果进行 `INNER JOIN`，条件是 script_id 和 timestamp 都匹配。
     * 3. 🔥 新增 `GROUP BY c.script_id`：这一步是关键，它能确保即使有多条消息共享同一个最新时间戳（罕见但可能），
     *    最终也只为每个剧本返回一条记录，从而彻底解决重复显示的问题。
     */
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT c.script_id as scriptId, c.content as lastMessage, c.timestamp as timestamp " +
           "FROM chat_messages c " +
           "INNER JOIN (SELECT script_id, MAX(timestamp) as max_timestamp FROM chat_messages GROUP BY script_id) s " +
           "ON c.script_id = s.script_id AND c.timestamp = s.max_timestamp " +
           "GROUP BY c.script_id " + // The fix to prevent duplicates
           "ORDER BY c.timestamp DESC")
    List<ChatSession> getAllChatSessions();
}