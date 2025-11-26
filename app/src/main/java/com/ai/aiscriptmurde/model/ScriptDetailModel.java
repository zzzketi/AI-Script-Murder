package com.ai.aiscriptmurde.model;
import java.io.Serializable;
import java.util.List;

public class ScriptDetailModel implements Serializable {
    //  基础信息
    private String id;
    private String title;
    private String desc;   // 列表页用的短描述
    private String image;
    private double score;
    private String difficulty;
    private List<String> tags;

    // 详情页扩展字段
    private BackgroundInfo background; // 背景信息
    private List<CharacterItem> characters; // 角色列表

    // === Getters ===
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDesc() { return desc; }
    public String getImage() { return image; }
    public double getScore() { return score; }
    public String getDifficulty() { return difficulty; }
    public List<String> getTags() { return tags; }
    public BackgroundInfo getBackground() { return background; }
    public List<CharacterItem> getCharacters() { return characters; }

    // === 内部类定义 ===

    /**
     * 剧本背景信息 (时间、地点、故事、规则)
     */
    public static class BackgroundInfo implements Serializable {
        private String time;
        private String location;
        private String story;     // 详细故事长文
        private List<String> rules; // 规则列表

        public String getTime() { return time; }
        public String getLocation() { return location; }
        public String getStory() { return story; }
        public List<String> getRules() { return rules; }
    }

    /**
     * 角色信息 (对应角色选择列表)
     */

}