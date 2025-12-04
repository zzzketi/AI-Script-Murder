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

            if (!msg.isUser) {
                db.chatSessionDao().incrementUnreadCount(msg.getScriptId());
            }
        });
    }
    
    public static void getSessionAndCreateIfNotExist(Context context, String scriptId, String scriptTitle, DataCallback<ChatSessionEntity> callback) {
        executor.execute(() -> {
            ChatSessionDao dao = AppDatabase.getInstance(context).chatSessionDao();
            ChatSessionEntity session = dao.getSessionById(scriptId);
            if (session == null) {
                // 🔥 优化：创建新会话时，最后一条消息默认为空
                session = new ChatSessionEntity(scriptId, scriptTitle, "", System.currentTimeMillis(), null);
                dao.insertOrReplaceSession(session);
            }
            final ChatSessionEntity finalSession = session;
            mainHandler.post(() -> callback.onSuccess(finalSession));
        });
    }

    public static LiveData<List<ChatSessionEntity>> getAllChatSessions(Context context) {
        return AppDatabase.getInstance(context).chatSessionDao().getAllSessions();
    }

    public static void clearUnreadCount(Context context, String scriptId) {
        executor.execute(() -> {
            AppDatabase.getInstance(context).chatSessionDao().clearUnreadCount(scriptId);
        });
    }

    public static void clearChatMessages(Context context, String scriptId, Runnable onCleared) {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            db.chatDao().clearHistory(scriptId);
            // 🔥 优化：清空后，最后一条消息更新为空
            db.chatSessionDao().updateSessionSummary(scriptId, "", System.currentTimeMillis());
            db.chatSessionDao().clearUnreadCount(scriptId);
            mainHandler.post(onCleared);
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

    public static void searchMessages(Context context, String scriptId, String query, DataCallback<List<ChatMessage>> callback) {
        executor.execute(() -> {
            String searchQuery = "%" + query + "%";
            List<ChatMessage> results = AppDatabase.getInstance(context).chatDao().searchMessages(scriptId, searchQuery);
            mainHandler.post(() -> callback.onSuccess(results));
        });
    }
}