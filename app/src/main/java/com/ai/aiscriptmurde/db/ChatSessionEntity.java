package com.ai.aiscriptmurde.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_sessions")
public class ChatSessionEntity {

    @PrimaryKey
    @NonNull
    private String scriptId;

    private String scriptTitle;

    private String lastMessage;

    private long timestamp;

    private String avatarUrl;

    private int unreadCount; // 🔥 新增：未读消息数字段

    public ChatSessionEntity(@NonNull String scriptId, String scriptTitle, String lastMessage, long timestamp, String avatarUrl) {
        this.scriptId = scriptId;
        this.scriptTitle = scriptTitle;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.avatarUrl = avatarUrl;
        this.unreadCount = 0; // 默认为0
    }

    // --- Getters and Setters ---

    @NonNull
    public String getScriptId() {
        return scriptId;
    }

    public void setScriptId(@NonNull String scriptId) {
        this.scriptId = scriptId;
    }

    public String getScriptTitle() {
        return scriptTitle;
    }

    public void setScriptTitle(String scriptTitle) {
        this.scriptTitle = scriptTitle;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}