package com.ai.aiscriptmurde.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {ChatMessage.class, ChatSessionEntity.class}, version = 3, exportSchema = false) // 🔥 版本升级到 3
public abstract class AppDatabase extends RoomDatabase {

    public abstract ChatDao chatDao();
    public abstract ChatSessionDao chatSessionDao();

    private static volatile AppDatabase INSTANCE;

    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(4);

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "jubensha_db")
                            .fallbackToDestructiveMigration() // 在实际应用中，您需要编写一个 Migration
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}