package com.ai.aiscriptmurde.ui.chat;

import android.os.Bundle;
import android.text.TextUtils;
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
import com.ai.aiscriptmurde.utils.DBHelper;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvChat;
    private ChatAdapter adapter;
    private EditText etInput;
    private String scriptId;
    private String systemPrompt;


    // UI 控件
    private TextView tvTitle;
    private ImageView ivBack;
    private Button btnSend;

    // 数据变量
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // 1. 获取传递过来的数据
        scriptId = getIntent().getStringExtra("SCRIPT_ID");
        if (scriptId == null) scriptId = "test_script_001"; // 默认测试ID

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
        ivBack = findViewById(R.id.iv_back);

        // 初始化 Adapter
        adapter = new ChatAdapter();
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(adapter);


        //后退逻辑
        ivBack.setOnClickListener(v -> finish());



        // 发送按钮点击事件
        btnSend.setOnClickListener(v -> {
            String content = etInput.getText().toString().trim();
            if (TextUtils.isEmpty(content)) {
                Toast.makeText(this, "不能发送空消息哦", Toast.LENGTH_SHORT).show();
                return;
            }
            // 执行发送逻辑
            sendMessage(content);
        });
    }

    private void loadHistory() {

        DBHelper.loadHistory(this, scriptId, history -> {
            // 这里已经是主线程了，直接更新 UI
            if (history != null && !history.isEmpty()) {
                adapter.setMessages(history);
                scrollToBottom();
            }
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
        DBHelper.insertMessage(this, userMsg);

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
//        Toast.makeText(this, "正在思考...", Toast.LENGTH_SHORT).show();
        new android.os.Handler().postDelayed(() -> {
            // 造一条 AI 消息
            ChatMessage aiMsg = new ChatMessage(scriptId, "管家(AI)", null, "我是模拟的AI回复，当你看到这条消息，说明你的Adapter和布局都写对了！", false);

            // 1. 存库
            AppDatabase.databaseWriteExecutor.execute(() -> {
                AppDatabase.getInstance(this).chatDao().insertMessage(aiMsg);
            });

            // 2. 显示
            adapter.addMessage(aiMsg);
            scrollToBottom();
        }, 1000); // 延迟1秒
    }
}