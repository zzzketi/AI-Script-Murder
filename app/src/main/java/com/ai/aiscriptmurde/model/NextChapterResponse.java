package com.ai.aiscriptmurde.model;

public class NextChapterResponse {
    private int chapter_index;
    private String title;
    private String narration;          // 剧情旁白
    private String discussion_question; // 开放性问题
    private String status;             // 状态，例如 "active", "voting", "completed"

    // Getters
    public int getChapterIndex() { return chapter_index; }
    public String getTitle() { return title; }
    public String getNarration() { return narration; }
    public String getDiscussionQuestion() { return discussion_question; }
    public String getStatus() { return status; }
}