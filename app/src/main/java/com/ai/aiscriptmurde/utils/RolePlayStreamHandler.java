package com.ai.aiscriptmurde.utils;


import android.util.Log;

import com.ai.aiscriptmurde.network.StreamCallback;
import com.ai.aiscriptmurde.network.StreamUiCallback;

public class RolePlayStreamHandler implements StreamCallback {

    private final StreamUiCallback uiCallback;

    // --- 内部状态变量 ---
    private StringBuilder lineBuffer = new StringBuilder();
    private StringBuilder fullRawContent = new StringBuilder();
    private boolean isNextDataRoleName = false;

    // 构造函数传入 UI 回调
    public RolePlayStreamHandler(StreamUiCallback uiCallback) {
        this.uiCallback = uiCallback;
    }

    @Override
    public void onStreamNext(String textChunk) {
        lineBuffer.append(textChunk);
        int newlineIndex;
        while ((newlineIndex = lineBuffer.indexOf("\n")) != -1) {
            String line = lineBuffer.substring(0, newlineIndex).trim();
            lineBuffer.delete(0, newlineIndex + 1);
            parseLine(line);
        }
    }

    private void parseLine(String line) {
        if (line.isEmpty()) return;

        // 1. 处理 Event
        if (line.startsWith("event:")) {
            String eventType = line.substring("event:".length()).trim();
            if ("role_info".equals(eventType)) {
                isNextDataRoleName = true;
            } else if ("done".equals(eventType)) {
                uiCallback.onComplete();
            }
            return;
        }

        // 2. 处理 Data
        if (line.startsWith("data:")) {
            String content = line.substring("data:".length());
            if (content.startsWith(" ")) content = content.substring(1);
            if ("[DONE]".equals(content.trim())) return;

            if (isNextDataRoleName) {
                // -> 通知 UI 更新名字
                uiCallback.onRoleNameUpdated(content.trim());
                isNextDataRoleName = false;
            } else {
                // -> 核心逻辑：追加 -> 清洗 -> 防闪烁判断
                processContent(content);
            }
        }
    }

    private void processContent(String newContent) {
        fullRawContent.append(newContent);
        String currentRaw = fullRawContent.toString();

        // 清洗
        String finalDisplayText = cleanAIResponse(currentRaw);

        // 防闪烁逻辑 (Anti-Flicker)
        boolean isDirtyHeaderBuilding = (currentRaw.trim().startsWith("[") || currentRaw.trim().startsWith("【"))
                && (finalDisplayText.trim().startsWith("[") || finalDisplayText.trim().startsWith("【"));

        // 只有不是正在构建的脏头时，才通知 UI 更新
        if (!isDirtyHeaderBuilding) {
            uiCallback.onContentUpdated(finalDisplayText);
        }
    }

    /**
     * 正则清洗逻辑
     */
    private String cleanAIResponse(String originalText) {
        if (originalText == null) return "";
        String result = originalText;

        result = result.replace("【等待其他角色发言】", "")
                .replace("[等待其他角色发言]", "");

        result = result.replaceAll("^\\s*([【\\[].*?[】\\]][:：]?\\s*)+", "");
        return result;
    }

    @Override
    public void onStreamComplete() {
        Log.d("AItest",fullRawContent.toString());
        uiCallback.onComplete();

    }

    @Override
    public void onError(Throwable t) {
        uiCallback.onError(t);
    }
}