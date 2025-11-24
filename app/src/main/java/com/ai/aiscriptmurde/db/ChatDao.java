package com.ai.aiscriptmurde.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ChatDao {

    // 1. 插入一条新消息
    @Insert
    void insertMessage(ChatMessage message);

    // 2. 根据剧本ID，加载该剧本的所有历史记录 (按时间顺序)
    @Query("SELECT * FROM chat_messages WHERE script_id = :scriptId ORDER BY timestamp ASC")
    List<ChatMessage> getHistoryByScriptId(String scriptId);

    // 3. 清空某个剧本的记录 (用于“重新开始”功能)
    @Query("DELETE FROM chat_messages WHERE script_id = :scriptId")
    void clearHistory(String scriptId);
}