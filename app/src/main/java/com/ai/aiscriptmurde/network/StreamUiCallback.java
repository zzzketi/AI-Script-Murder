package com.ai.aiscriptmurde.network;

public interface StreamUiCallback {
    // 当解析出角色名时调用
    void onRoleNameUpdated(String roleName);

    // 当解析出正文，且清洗完毕需要刷新 UI 时调用
    void onContentUpdated(String finalContent);

    // 流结束
    void onComplete();

    // 出错
    void onError(Throwable t);
}