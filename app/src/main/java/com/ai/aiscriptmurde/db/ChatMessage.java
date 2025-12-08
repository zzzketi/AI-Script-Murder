package com.ai.aiscriptmurde.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "chat_messages")
public class ChatMessage implements Serializable {

    // --- 消息方向/归属 ---
    public static final int TYPE_USER = 1;
    public static final int TYPE_PLOT = 2;
    public static final int TYPE_SYSTEM = 3;

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "session_id") // 3. 关联的会话ID (关键！)
    private String sessionId;

    @ColumnInfo(name = "script_id")
    public String scriptId;

    @ColumnInfo(name = "sender_name")
    public String senderName;

    @ColumnInfo(name = "role_id")
    public String roleId;

    @ColumnInfo(name = "content")
    public String content;

    @ColumnInfo(name = "avatar_url")
    public String avatarUrl;

    @ColumnInfo(name = "type")
    int type;

    @ColumnInfo(name = "is_user")
    public boolean isUser;

    public long timestamp;

//    public ChatMessage(String scriptId, String senderName, String roleId, String content, boolean isUser,int type) {
//        this.scriptId = scriptId;
//        this.senderName = senderName;
//        this.roleId = roleId;
//        this.content = content;
//        this.isUser = isUser;
//        this.timestamp = System.currentTimeMillis();
//        this.type = type;
//    }
    //用户和角色的
    @Ignore
    public ChatMessage(String scriptId, String sessionId, String roleId, String senderName,String avatarUrl, String content, int type) {
        this.scriptId = scriptId;
        this.sessionId = sessionId;
        this.roleId = roleId;
        this.senderName = senderName;
        this.avatarUrl = avatarUrl;
        this.content = content;
        this.type = type;
        this.timestamp = System.currentTimeMillis();

        // 自动判断 isUser
        this.isUser = (type == TYPE_USER);
    }

    //系统的
    @Ignore
    public ChatMessage(String scriptId, String sessionId, String content, int type) {
        this.scriptId = scriptId;
        this.sessionId = sessionId;
        this.content = content;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        // 自动判断 isUser
        this.isUser = false;
    }

    public ChatMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    // --- Getters ---

    public String getScriptId() {
        return scriptId;
    }

    public String getSessionId() {
        return sessionId;
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

    public int getType() {
        return type;
    }

    public void setContent(String s) {
        this.content = s;
    }

    public void setScriptId(String scriptId) {
        this.scriptId = scriptId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}