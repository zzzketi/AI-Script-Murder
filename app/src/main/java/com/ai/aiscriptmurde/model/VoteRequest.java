package com.ai.aiscriptmurde.model;

import com.google.gson.annotations.SerializedName;

// 新建一个类 VoteRequest.java
public class VoteRequest {
    @SerializedName("target_role_id") // 确保字段名和 API 文档一致
    private String targetRoleId;

    public VoteRequest(String targetRoleId) {
        this.targetRoleId = targetRoleId;
    }
}