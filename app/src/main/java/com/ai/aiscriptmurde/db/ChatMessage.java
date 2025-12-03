package com.ai.aiscriptmurde.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "chat_messages")
public class ChatMessage implements Serializable {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "script_id")
    public String scriptId;

    @ColumnInfo(name = "sender_name")
    public String senderName;

    @ColumnInfo(name = "role_id")
    public String roleId;

    public String content;

    @ColumnInfo(name = "is_user")
    public boolean isUser;

    public long timestamp;

    public ChatMessage(String scriptId, String senderName, String roleId, String content, boolean isUser) {
        this.scriptId = scriptId;
        this.senderName = senderName;
        this.roleId = roleId;
        this.content = content;
        this.isUser = isUser;
        this.timestamp = System.currentTimeMillis();
    }

    // --- Getters ---

    public String getScriptId() {
        return scriptId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }
}