package com.ai.aiscriptmurde.model;

import androidx.room.Ignore;

public class ChatSession {

    private String scriptId;
    private String lastMessage;
    private long timestamp;

    @Ignore
    private String scriptTitle;
    @Ignore
    private String avatarUrl;

    /**
     * This constructor is used by Room to create objects from the query result.
     * It only contains fields that are returned by the DAO query.
     */
    public ChatSession(String scriptId, String lastMessage, long timestamp) {
        this.scriptId = scriptId;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
    }

    // Getters and Setters for all fields

    public String getScriptId() {
        return scriptId;
    }

    public void setScriptId(String scriptId) {
        this.scriptId = scriptId;
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

    public String getScriptTitle() {
        return scriptTitle;
    }

    public void setScriptTitle(String scriptTitle) {
        this.scriptTitle = scriptTitle;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}