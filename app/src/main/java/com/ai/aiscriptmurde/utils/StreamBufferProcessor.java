package com.ai.aiscriptmurde.utils;
import android.os.Handler;
import android.os.Looper;

import com.ai.aiscriptmurde.utils.MultiRoleStreamHandler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


import android.os.Handler;
import android.os.Looper;

// 确保这里引用的包名是正确的，如果 MultiRoleStreamHandler 和这个类在同一个包，可以删掉这行 import
// import com.ai.aiscriptmurde.utils.MultiRoleStreamHandler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StreamBufferProcessor {

    private final MultiRoleStreamHandler.MultiRoleCallback callback;
    private final StringBuilder buffer = new StringBuilder();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // 正则解释（严格模式）：
    // (.*?)           -> Group 1: 捕获角色标记之前的所有内容（即上一句台词）
    // (?:\n|^|\s*)    -> 非捕获组：匹配换行、开头或可能的空格（吃掉名字前的格式干扰）
    // [【\[]          -> 匹配左括号 【 或 [
    // \s* -> 允许名字前有空格
    // (.+?)           -> Group 2: 捕获角色名字
    // \s* -> 允许名字后有空格
    // [】\]]          -> 匹配右括号 】 或 ]
    // \s*[:：]        -> 【关键】必须紧跟着冒号！否则不会被识别为角色
    private static final Pattern ROLE_PATTERN = Pattern.compile(
            // 前缀部分保持不变
            "(.*?)(?:\\n|^|\\s*)" +

                    // 匹配左括号
                    "[【\\[]\\s*" +

                    // 🚨 核心修改在这里！
                    // 原来是 (.+?) -> 允许包含括号，导致跨行吞并
                    // 现在是 ([^【\\[】\\]]+) -> 只要遇到括号就强制停止！
                    "([^【\\[】\\]]+)" +

                    // 匹配右括号
                    "\\s*[】\\]]\\s*" +

                    // 冒号依然保持必选 (不加 ?)，严格遵守你的逻辑
                    "[:：]",

            Pattern.DOTALL
    );

    public StreamBufferProcessor(MultiRoleStreamHandler.MultiRoleCallback callback) {
        this.callback = callback;
    }

    public void appendAndProcess(String chunk) {
        if (chunk == null) return;

        // 1. 过滤掉干扰标记 [START] 和 [DONE]
        // 这一步是为了防止这些系统标记出现在文本中，但即使不过滤，正则也不会把它们识别为角色（因为没有冒号）
        if (chunk.contains("[START]") || chunk.contains("[DONE]")) {
            chunk = chunk.replace("[START]", "").replace("[DONE]", "");
        }

        if (chunk.isEmpty()) return;

        buffer.append(chunk);
        scanBuffer();
    }

    private void scanBuffer() {
        String currentText = buffer.toString();
        Matcher matcher = ROLE_PATTERN.matcher(currentText);

        // 循环查找：缓冲区里可能一口气包含了多个角色的切换
        // 只有当找到了 "【名字】： " 这种完整结构时，matcher.find() 才会返回 true
        while (matcher.find()) {
            String prevContent = matcher.group(1); // 冒号前面的所有内容（包括可能的动作描述如 [拿起枪]）
            String roleName = matcher.group(2);    // 提取出的角色名

            // 1. 如果有上一段台词，追加给当前正在说话的人
            if (prevContent != null && !prevContent.isEmpty()) {
                final String finalContent = prevContent;
                // 只有当不是第一句话（导致prevContent为空）时才追加
                mainHandler.post(() -> callback.onAppendContent(finalContent));
            }

            // 2. 切换新角色（创建新气泡）
            if (roleName != null) {
                final String finalRole = roleName.trim();
                mainHandler.post(() -> callback.onSwitchRole(finalRole));
            }

            // 3. 从缓冲区删除已处理的部分（直到冒号结束的位置）
            // 例如缓冲区是："你好。[侦探]:"，处理完后缓冲区清空，等待下一句
            buffer.delete(0, matcher.end());

            // 重置匹配器，继续检查剩余的字符串（防止一次发来两句话）
            matcher.reset(buffer.toString());
        }


        String checkContent = buffer.toString();

        // 【关键修改】同时检查中文括号 "【" 和英文括号 "["
        // 只有当缓冲区里完全没有“可能是名字起始符”的符号时，才安全吐出
        boolean hasPotentialRoleTag = checkContent.contains("【") || checkContent.contains("[");

        if (!hasPotentialRoleTag) {
            // 没有括号，说明全是普通文本，全部上屏
            String text = buffer.toString();
            buffer.setLength(0); // 清空
            mainHandler.post(() -> callback.onAppendContent(text));
        }
            // 如果 hasPotentialRoleTag 为 true，说明缓冲区里有一个 "["，
            // 可能是正在传输 "[侦探]:"，所以我们要继续等待冒号的到来，暂不上屏

    }

    // 流结束时，把缓冲区里剩下的所有话都给当前角色
    public void flushRemaining() {
        if (buffer.length() > 0) {
            String remaining = buffer.toString();
            buffer.setLength(0);
            mainHandler.post(() -> callback.onAppendContent(remaining));
        }
    }
}