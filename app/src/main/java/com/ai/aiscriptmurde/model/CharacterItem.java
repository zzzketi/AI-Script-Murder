package com.ai.aiscriptmurde.model;

import java.io.Serializable;

public class CharacterItem implements Serializable {
    private String id;
    private String name;
    private String avatar;
    private String age;
    private String desc; // 角色具体介绍
    private String introduction; // 角色详细介绍



    public String getId() { return id; }
    public String getName() { return name; }
    public String getAvatar() { return avatar; }
    public String getAge() { return age; }
    public String getDesc() { return desc; }
    public String getIntroduction() { return introduction; }

    public CharacterItem(String id, String name, String avatar, String age, String desc, String introduction) {
        this.id = id;
        this.name = name;
        this.avatar = avatar;
        this.age = age;
        this.desc = desc;
        this.introduction = introduction;
    }

    public CharacterItem(){

    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }
}