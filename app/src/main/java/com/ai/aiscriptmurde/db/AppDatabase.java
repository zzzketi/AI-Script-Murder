package com.ai.aiscriptmurde.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {ChatMessage.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract ChatDao chatDao();

    private static volatile AppDatabase INSTANCE;

    // ✅ 新增：定义一个固定的线程池（这里用4个线程）来处理后台任务
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(4);

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "jubensha_db")
                            // ❌ 删掉这一行：.allowMainThreadQueries()
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

/*
插入数据：

AppDatabase.databaseWriteExecutor.execute(() -> {
    // 这里的代码会在后台线程运行
    chatDao.insertMessage(msg);
});
 */


/*
读取数据：
// 1. 获取数据库实例
AppDatabase db = AppDatabase.getInstance(this);

// 2. 在后台开启查询
AppDatabase.databaseWriteExecutor.execute(() -> {
    // 【后台线程】查询数据
    List<ChatMessage> historyList = db.chatDao().getHistoryByScriptId("s_001");

    // 3. 拿到数据后，切换回主线程更新 UI
    runOnUiThread(() -> {
        // 【主线程】更新 UI
        // 假设你有 adapter 和 list
        myMessageList.clear();
        myMessageList.addAll(historyList);
        myAdapter.notifyDataSetChanged();

        // 滚到底部
        recyclerView.scrollToPosition(myMessageList.size() - 1);
    });
});
 */