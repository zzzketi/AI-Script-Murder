package com.ai.aiscriptmurde.model;

public class CreateSessionRequest {
    private String script_id;
    private String user_role_id;
    private String model_name;

    public CreateSessionRequest(String script_id, String user_role_id, String model_name) {
        this.script_id = script_id;
        this.user_role_id = user_role_id;
        this.model_name = model_name;
    }
}