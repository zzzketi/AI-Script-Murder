package com.ai.aiscriptmurde.model;

/**
 * 搜证请求模型
 * 发送给后端：我想搜查哪里？
 */
public class SearchRequest {
    // 1. 发起搜证的用户ID
    public String userId;

    // 2. 目标地点标识 (例如: "kings_room", "garden", "butler_room")
    // 这个字符串必须和后端数据库里的地点 key 对应
    public String location;

    public SearchRequest(String userId, String location) {
        this.userId = userId;
        this.location = location;
    }
}