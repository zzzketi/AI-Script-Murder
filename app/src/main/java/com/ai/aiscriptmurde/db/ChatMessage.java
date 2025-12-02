package com.ai.aiscriptmurde.db;



import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "chat_messages")
public class ChatMessage implements Serializable {

    @PrimaryKey(autoGenerate = true)
    public int id;

    // 关联的剧本ID (对应 JSON 里的 id)
    @ColumnInfo(name = "script_id")
    public String scriptId;

    // 角色名称 (用于显示在气泡上方)
    @ColumnInfo(name = "sender_name")
    public String senderName;

    // 角色ID (可选，用于将来扩展查找头像)
    @ColumnInfo(name = "role_id")
    public String roleId;

    // 消息内容
    public String content;

    // 是否是当前用户发送 (决定气泡左右)
    @ColumnInfo(name = "is_user")
    public boolean isUser;

    // 时间戳
    public long timestamp;

    // 构造函数
    public ChatMessage(String scriptId, String senderName, String roleId, String content, boolean isUser) {
        this.scriptId = scriptId;
        this.senderName = senderName;
        this.roleId = roleId;
        this.content = content;
        this.isUser = isUser;
        this.timestamp = System.currentTimeMillis();
    }
}