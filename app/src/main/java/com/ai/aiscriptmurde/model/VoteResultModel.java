package com.ai.aiscriptmurde.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class VoteResultModel {

    // 1. 投票统计列表
    @SerializedName("vote_counts")
    private List<VoteCount> voteCounts;

    // 2. AI 玩家的投票详情（包含推理过程）
    @SerializedName("ai_votes")
    private List<AiVote> aiVotes;

    // 3. 真相/复盘内容
    @SerializedName("truth")
    private String truth;

    // --- Getters ---
    public List<VoteCount> getVoteCounts() {
        return voteCounts;
    }

    public List<AiVote> getAiVotes() {
        return aiVotes;
    }

    public String getTruth() {
        return truth;
    }

    // ==========================================
    // 内部类 1: 票数统计
    // ==========================================
    public static class VoteCount {
        @SerializedName("role_name")
        private String roleName;

        @SerializedName("count")
        private int count;

        public String getRoleName() {
            return roleName;
        }

        public int getCount() {
            return count;
        }
    }

    // ==========================================
    // 内部类 2: AI 投票详情
    // ==========================================
    public static class AiVote {
        @SerializedName("voter_role_id")
        private String voterRoleId;

        @SerializedName("voter_name")
        private String voterName;

        @SerializedName("target_role_name")
        private String targetRoleName;

        @SerializedName("reasoning")
        private String reasoning;

        public String getVoterRoleId() {
            return voterRoleId;
        }

        public String getVoterName() {
            return voterName;
        }

        public String getTargetRoleName() {
            return targetRoleName;
        }

        public String getReasoning() {
            return reasoning;
        }
    }
}