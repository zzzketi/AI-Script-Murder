package com.ai.aiscriptmurde.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;

import com.ai.aiscriptmurde.db.AppDatabase;
import com.ai.aiscriptmurde.db.ChatMessage;
import com.ai.aiscriptmurde.db.ChatSessionDao;
import com.ai.aiscriptmurde.db.ChatSessionEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DBHelper {

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void loadHistory(Context context, String scriptId, DataCallback<List<ChatMessage>> callback) {
        executor.execute(() -> {
            List<ChatMessage> history = AppDatabase.getInstance(context).chatDao().getHistoryByScriptId(scriptId);
            mainHandler.post(() -> callback.onSuccess(history));
        });
    }

    public static void insertMessage(Context context, ChatMessage msg) {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            db.chatDao().insertMessage(msg);
            
            String lastMessage = msg.getSenderName() + ": " + msg.getContent();
            db.chatSessionDao().updateSessionSummary(msg.getScriptId(), lastMessage, msg.getTimestamp());

            // 🔥 核心逻辑：如果不是用户自己发的消息，则未读数 +1
            if (!msg.isUser) {
                db.chatSessionDao().incrementUnreadCount(msg.getScriptId());
            }
        });
    }

    public static void createSessionIfNotExists(Context context, String scriptId, String scriptTitle) {
        executor.execute(() -> {
            ChatSessionDao dao = AppDatabase.getInstance(context).chatSessionDao();
            ChatSessionEntity session = dao.getSessionById(scriptId);
            if (session == null) {
                ChatSessionEntity newSession = new ChatSessionEntity(scriptId, scriptTitle, "点击开始对话...", System.currentTimeMillis(), null);
                dao.insertOrReplaceSession(newSession);
            }
        });
    }

    public static LiveData<List<ChatSessionEntity>> getAllChatSessions(Context context) {
        return AppDatabase.getInstance(context).chatSessionDao().getAllSessions();
    }

    /**
     * 🔥 新增：暴露给UI层，用于清空指定会话的未读数
     */
    public static void clearUnreadCount(Context context, String scriptId) {
        executor.execute(() -> {
            AppDatabase.getInstance(context).chatSessionDao().clearUnreadCount(scriptId);
        });
    }

    public static void deleteChatHistory(Context context, String scriptId, Runnable onDeleted) {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            db.chatDao().clearHistory(scriptId);
            db.chatSessionDao().deleteSessionById(scriptId);
            mainHandler.post(onDeleted);
        });
    }
}