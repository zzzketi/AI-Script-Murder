package com.ai.aiscriptmurde.utils;


import android.os.Handler;
import android.os.Looper;
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

    private static void readStream(ResponseBody body, MultiRoleCallback callback) {
        try {
            java.io.InputStream is = body.byteStream();
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is));

            char[] buffer = new char[1024];
            int len;

            // 这是一个 "StringBuilder"，用来存还没处理完的文本
            StringBuilder textBuffer = new StringBuilder();

            while ((len = reader.read(buffer)) != -1) {
                String chunk = new String(buffer, 0, len);

                // 处理 SSE 的 "data:" 前缀 (如果你的流是 SSE 格式，必须先去头)
                // 这里假设你已经去掉 data: 前缀，或者是非 SSE 的纯文本流
                // 为了通用性，我这里写一段简易的 SSE 清洗逻辑，如果不需要可以删掉
                chunk = cleanSSE(chunk);

                textBuffer.append(chunk);

                // --- 核心处理逻辑 ---
                processBuffer(textBuffer, callback);
            }

            // 流结束了，把 buffer 里剩下的所有东西都吐出来
            if (textBuffer.length() > 0) {
                String remain = textBuffer.toString();
                mainHandler.post(() -> callback.onAppendContent(remain));
            }
            mainHandler.post(callback::onComplete);

        } catch (Exception e) {
            mainHandler.post(() -> callback.onError(e));
        }
    }

    // 扫描 buffer，切分角色
    private static void processBuffer(StringBuilder buffer, MultiRoleCallback callback) {
        Matcher matcher = ROLE_PATTERN.matcher(buffer);

        // 循环查找 buffer 里所有的 "【名字】："
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            // 1. 提取标记前面的内容（属于上一个角色）
            if (start > 0) {
                String prevContent = buffer.substring(0, start);
                // 去掉开头的换行符（如果上一个角色说完话，会有个换行）
                if (prevContent.startsWith("\n")) prevContent = prevContent.substring(1);

                final String finalPrev = prevContent;
                if (!finalPrev.isEmpty()) {
                    mainHandler.post(() -> callback.onAppendContent(finalPrev));
                }
            }

            // 2. 提取新角色名字
            String nameGroup1 = matcher.group(1); // 带括号的
            String nameGroup2 = matcher.group(2); // 不带括号的
            String roleName = (nameGroup1 != null) ? nameGroup1 : nameGroup2;

            final String finalRoleName = roleName.trim();
            mainHandler.post(() -> callback.onSwitchRole(finalRoleName));

            // 3. 从 buffer 中删除已处理的部分（包括标记本身）
            buffer.delete(0, end);

            // 重置 matcher，因为 buffer 变了
            matcher = ROLE_PATTERN.matcher(buffer);
        }

        // --- 防截断缓冲逻辑 ---
        // 循环结束后，buffer 里可能还剩一些字。
        // 如果剩下的字里包含 '[' 或 '【' 或 '\n'，可能是下一个标记的一半，比如 "[路人"
        // 这种情况下，我们先不显示，等下一波数据来了拼全了再说。

        String remain = buffer.toString();
        // 简单的判断：如果末尾看起来像标记的开头，保留最后 10 个字符
        // 否则，除了最后一点点防抖动外，其他的都上屏

        int safeLength = remain.length();
        // 如果包含潜在的标记头，我们在那个头之前截断
        int potentialTagIndex = Math.max(remain.lastIndexOf('\n'), Math.max(remain.lastIndexOf('【'), remain.lastIndexOf('[')));

        if (potentialTagIndex != -1 && (remain.length() - potentialTagIndex) < 10) {
            // 看起来像是有个标签没传完，只上屏标签前面的
            safeLength = potentialTagIndex;
        }

        if (safeLength > 0) {
            String safeContent = remain.substring(0, safeLength);
            buffer.delete(0, safeLength); // 从 buffer 移除
            mainHandler.post(() -> callback.onAppendContent(safeContent));
        }
    }

    // 简单的 SSE 清洗 (根据你的实际流格式调整)
    private static String cleanSSE(String raw) {
        // 如果你的后端返回的是纯文本，直接 return raw;
        // 如果是 data: xxx，在这里 replace
        return raw.replace("data:", "").replace("event:role_info", "");
        // 注意：这里只是简单示例，最好用之前的 lineBuffer 按行解析法
    }
}