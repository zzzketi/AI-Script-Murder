package com.ai.aiscriptmurde.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.ai.aiscriptmurde.db.ChatMessage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIUtils {

    // 1. 配置 OkHttp (设置超时时间为 30秒，因为 AI 思考比较慢)
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    // 2. 你的 API Key (去 DeepSeek 官网申请，填在这里)
    private static final String API_KEY = "sk-pcvejvjggeufpcxuvynganzcunyahpwfewcafmooaleysmtj";
    // DeepSeek 的 API 地址
    private static final String API_URL = "https://api.siliconflow.cn/v1/chat/completions";

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    /**
     * 发送消息给 AI
     * @param systemPrompt 剧本的核心设定 (JSON里的 system_prompt)
     * @param history 用户说的话
     * @param callback 回调接口，返回 AI 的回复内容
     */
    public static void chatWithAI(String systemPrompt, List<ChatMessage> history, DataCallback<String> callback) {

        // A. 拼装 JSON 请求体 (这是发给 DeepSeek 的格式)
        // 格式参考：{"model":"deepseek-chat", "messages": [...]}
        JSONObject jsonBody = new JSONObject();
        try {


            JSONArray messages = new JSONArray();
            jsonBody.put("model", "deepseek-ai/DeepSeek-V3");

            // 1. 系统设定 (System)
            JSONObject sysMsg = new JSONObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            messages.put(sysMsg);


            // 2. 遍历历史记录 (合并连续的角色消息)
            if (history != null && !history.isEmpty()) {
                int maxHistory = 20;
                int start = Math.max(0, history.size() - maxHistory);

                for (int i = start; i < history.size(); i++) {
                    ChatMessage msg = history.get(i);
                    if (msg.content == null) continue;

                    // 当前消息的角色
                    String currentRole = msg.isUser ? "user" : "assistant";

                    // 当前消息的内容
                    String currentContent = msg.content;
                    if (!msg.isUser && msg.senderName != null && !currentContent.startsWith("[")) {
                        currentContent = "[" + msg.senderName + "]: " + currentContent;
                    }

                    // 🔥【核心修复逻辑】检查上一条消息
                    if (messages.length() > 0) {
                        JSONObject lastJsonMsg = messages.getJSONObject(messages.length() - 1);
                        String lastRole = lastJsonMsg.optString("role");

                        // 如果当前角色 == 上一条的角色 (比如都是 assistant)
                        if (currentRole.equals(lastRole)) {
                            // 🤝 合并！把内容拼接到上一条后面，用换行符隔开
                            String oldContent = lastJsonMsg.getString("content");
                            lastJsonMsg.put("content", oldContent + "\n\n" + currentContent);
                            // 跳过本次循环，不添加新条目
                            continue;
                        }
                    }

                    // 如果角色不一样，才添加新条目
                    JSONObject jsonMsg = new JSONObject();
                    jsonMsg.put("role", currentRole);
                    jsonMsg.put("content", currentContent);
                    messages.put(jsonMsg);
                }
            }
            jsonBody.put("messages", messages);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // B. 创建 Request
        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .post(body)
                .build();






        // --- C. 发送请求 ---
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 🔥 失败：调用 onFailure
                returnFailure(callback, "网络连接失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        // 解析返回的 JSON
                        String respStr = response.body().string();
                        JSONObject respJson = new JSONObject(respStr);

                        // 提取 AI 回复的内容
                        // 结构通常是 choices[0].message.content
                        String aiText = respJson.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");

                        // 切回主线程
                        mainHandler.post(() -> callback.onSuccess(aiText));

                    } catch (Exception e) {
                        e.printStackTrace();
                        mainHandler.post(() -> callback.onFailure("解析出错了"));
                    }
                } else {
                    // 🔥 API 报错：调用 onFailure
                    returnFailure(callback, "服务器报错: " + response.code());
                }
            }
        });
    }

    // ✅ 辅助方法1：返回成功
    private static void returnSuccess(DataCallback<String> callback, String result) {
        mainHandler.post(() -> {
            if (callback != null) callback.onSuccess(result);
        });
    }

    // ❌ 辅助方法2：返回失败
    private static void returnFailure(DataCallback<String> callback, String errorMsg) {
        mainHandler.post(() -> {
            if (callback != null) callback.onFailure(errorMsg);
        });
    }
}