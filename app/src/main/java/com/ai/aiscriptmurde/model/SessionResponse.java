package com.ai.aiscriptmurde.model;

public class SessionResponse {
    private String session_id;
    private int current_chapter_index;
    // 如果后端还有其他字段，比如 created_at，也可以加在这里

    public String getSessionId() {
        return session_id;
    }

    public int getCurrent_chapter_index() {
        return current_chapter_index;
    }
}