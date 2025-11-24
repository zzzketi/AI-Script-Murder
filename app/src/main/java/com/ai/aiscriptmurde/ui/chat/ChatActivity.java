package com.ai.aiscriptmurde.ui.chat;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ai.aiscriptmurde.R;
import com.ai.aiscriptmurde.db.AppDatabase;
import com.ai.aiscriptmurde.db.ChatMessage;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvChat;
    private ChatAdapter adapter;
    private EditText etInput;
    private String scriptId;
    private String systemPrompt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 1. 获取传递过来的数据
        scriptId = getIntent().getStringExtra("SCRIPT_ID");
        systemPrompt = getIntent().getStringExtra("SYSTEM_PROMPT");
        String title = getIntent().getStringExtra("SCRIPT_TITLE");

        // 2. 初始化控件
        initViews(title);

        // 3. 加载历史记录
        loadHistory();
    }

    private void initViews(String title) {
        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText(title == null ? "剧本杀" : title);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        rvChat = findViewById(R.id.rv_chat);
        etInput = findViewById(R.id.et_input);
        Button btnSend = findViewById(R.id.btn_send);

        // 初始化 Adapter
        adapter = new ChatAdapter();
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(adapter);

        // 发送按钮逻辑
        btnSend.setOnClickListener(v -> {
            String content = etInput.getText().toString().trim();
            if (!content.isEmpty()) {
                sendMessage(content);
            }
        });
    }

    private void loadHistory() {
        // 后台查库
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<ChatMessage> history = AppDatabase.getInstance(this).chatDao().getHistoryByScriptId(scriptId);
            runOnUiThread(() -> {
                adapter.setMessages(history);
                scrollToBottom();
            });
        });
    }

    private void sendMessage(String content) {
        // 1. 清空输入框
        etInput.setText("");

        // 2. 构建消息对象
        ChatMessage userMsg = new ChatMessage(scriptId, "我", null, content, true);

        // 3. 更新 UI (立刻显示，不用等数据库)
        adapter.addMessage(userMsg);
        scrollToBottom();

        // 4. 存入数据库 (后台)
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase.getInstance(this).chatDao().insertMessage(userMsg);
        });

        // 5. 🔥 呼叫 AI (下一步做)
        callAI(content);
    }

    private void scrollToBottom() {
        if (adapter.getItemCount() > 0) {
            rvChat.smoothScrollToPosition(adapter.getItemCount() - 1);
        }
    }

    private void callAI(String userContent) {
        // 暂时留空，第三步填坑
        Toast.makeText(this, "正在思考...", Toast.LENGTH_SHORT).show();
    }
}