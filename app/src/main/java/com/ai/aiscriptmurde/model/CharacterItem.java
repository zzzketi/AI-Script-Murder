package com.ai.aiscriptmurde.model;

import java.io.Serializable;

public class CharacterItem implements Serializable {
        private String id;
        private String name;
        private String avatar;
        private String age;
        private String desc; // 角色具体介绍

        public String getId() { return id; }
        public String getName() { return name; }
        public String getAvatar() { return avatar; }
        public String getAge() { return age; }
        public String getDesc() { return desc; }
    }