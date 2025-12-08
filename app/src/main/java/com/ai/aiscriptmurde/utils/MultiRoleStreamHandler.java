package com.ai.aiscriptmurde.utils;


import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MultiRoleStreamHandler {

    // 回调接口：通知 Activity 干活
    public interface MultiRoleCallback {
        // 创建一个新角色的气泡
        void onSwitchRole(String roleName);
        // 给当前最后一个气泡追加文字
        void onAppendContent(String content);
        // 结束
        void onComplete();
        void onError(Throwable t);
        // 【修改】增加参数，返回完整的原始数据字符串
        void onComplete(String fullRawContent);
    }

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    // 正则：匹配 \n[名字]: 或 ^[名字]:
    // Group 1 = 带括号的名字, Group 2 = 不带括号的名字
    private static final Pattern ROLE_PATTERN = Pattern.compile("(?:^|\\n)\\s*(?:[【\\[](.+?)[】\\]]|(.+?))\\s*[:：]");

    public static void handle(Call<ResponseBody> call, MultiRoleCallback callback) {
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    new Thread(() -> readStream(response.body(), callback)).start();
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Error: " + response.code())));
                }
            }


            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                mainHandler.post(() -> callback.onError(t));
            }
        });
    }

    // 在 MultiRoleStreamHandler 类中

    private static void readStream(ResponseBody body, MultiRoleCallback callback) {
        // 1. 创建流读取器
        BufferedReader reader = new BufferedReader(new InputStreamReader(body.byteStream()));
        String line;

        // 2. 【新增】完整数据累加器 (只负责记录，不做任何处理)
        StringBuilder fullContentRecorder = new StringBuilder();

        // 初始化处理器
        StreamBufferProcessor processor = new StreamBufferProcessor(callback);

        try {

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data:")) {
                    // 截取 data: 后面的内容，保留原始空格
                    // 有些流数据是 "data:  我"，这里要注意 substring(5) 可能会吃掉一个空格
                    // 建议安全截取：
                    String raw = line.length() > 5 ? line.substring(5) : "";

                    boolean isBlank = raw.trim().isEmpty();

                    String content;
                    if (isBlank && raw.length() > 0) {
                        content = raw;  // 保留模型输出的空格
                    } else {
                        content = raw.trim(); // 去掉前后空格
                    }


                   processor.appendAndProcess(content);
                }
            }

            // 流结束，强制清空缓冲区剩余内容上屏
            processor.flushRemaining();

            // 通知 UI 结束
            new Handler(Looper.getMainLooper()).post(callback::onComplete);

        } catch (IOException e) {
            new Handler(Looper.getMainLooper()).post(() -> callback.onError(e));
        } finally {
            try { body.close(); } catch (Exception ignored) {}
        }
    }

}