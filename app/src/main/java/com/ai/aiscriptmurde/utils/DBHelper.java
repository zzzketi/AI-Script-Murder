package com.ai.aiscriptmurde.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.ai.aiscriptmurde.db.AppDatabase;
import com.ai.aiscriptmurde.db.ChatMessage;

import java.util.List;

public class DBHelper {

    // 获取主线程的 Handler，用来发消息给 UI 线程
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * 封装 1：查询历史记录
     * @param context 上下文
     * @param scriptId 剧本ID
     * @param callback 回调接口（拿到数据后干嘛）
     */
    public static void loadHistory(Context context, String scriptId, DataCallback<List<ChatMessage>> callback) {
        // 1. 在后台线程执行查询
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // 查库
            List<ChatMessage> history = AppDatabase.getInstance(context)
                    .chatDao().getHistoryByScriptId(scriptId);

            // 2. 切回主线程
            mainHandler.post(() -> {
                if (callback != null) {
                    // ❌ 原来是: callback.onResult(history);
                    // ✅ 现在改成:
                    callback.onSuccess(history);
                }
            });
        });
    }

    /**
     * 封装 2：插入消息 (不需要回调，发完不管)
     */
    public static void insertMessage(Context context, ChatMessage msg) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase.getInstance(context).chatDao().insertMessage(msg);
        });
    }
}