package com.ai.aiscriptmurde.model;

import java.util.List;

/**
 * created by wch on 2025/11/25.
 * ScriptModel 用于表示剧本的信息
 */
public class ScriptModel {
    private String id;   //  001
    private String title;   // 题目 例如 "豪门复仇"
    private String desc;    // 描述 例如 "你是一个被家族背叛的继承人，决心复仇..."
    private String image;      // 例如 "cover_001"
    private double score;      // 例如 9.2
    private String difficulty; // "新手"
    private List<String> tags; // ["豪门", "复仇"]


    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDesc() { return desc; }
    public String getImage() { return image; }
    public double getScore() { return score; }
    public String getDifficulty() { return difficulty; }
    public List<String> getTags() { return tags; }
}