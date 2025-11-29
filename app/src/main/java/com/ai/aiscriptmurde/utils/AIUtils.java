package com.ai.aiscriptmurde.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
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
     * @param userMessage 用户刚才说的话
     * @param callback 回调接口，返回 AI 的回复内容
     */
    public static void chatWithAI(String systemPrompt, String userMessage, DataCallback<String> callback) {

        // A. 拼装 JSON 请求体 (这是发给 DeepSeek 的格式)
        // 格式参考：{"model":"deepseek-chat", "messages": [...]}
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "deepseek-ai/DeepSeek-V3");
            JSONArray messages = new JSONArray();
            // 第一条：系统设定 (你是谁，剧本是什么)
            JSONObject sysMsg = new JSONObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            messages.put(sysMsg);

            // 第二条：用户的话
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.put(userMsg);

            jsonBody.put("messages", messages);
            jsonBody.put("stream", false); // 暂时不用流式，简单点

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