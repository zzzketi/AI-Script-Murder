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
import com.ai.aiscriptmurde.model.CharacterItem;
import com.ai.aiscriptmurde.utils.AIUtils;
import com.ai.aiscriptmurde.utils.DBHelper;
import com.ai.aiscriptmurde.utils.DataCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvChat;
    private ChatAdapter adapter;
    private EditText etInput;


    // UI 控件
    private TextView tvTitle;
    private ImageView ivBack;
    private Button btnSend;


    // 数据变量
    private String scriptId;
    private String systemPrompt;
    private String userRoleName = "玩家"; // 默认名字
    // 数据变量
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        scriptId = getIntent().getStringExtra("SCRIPT_ID");
        if (scriptId == null) scriptId = "default_id";

        // 接收基础 Prompt
        String originalPrompt = getIntent().getStringExtra("SYSTEM_PROMPT");
        if (originalPrompt == null) originalPrompt = "你是剧本杀主持人。";

        String title = getIntent().getStringExtra("SCRIPT_TITLE");

        // 🔥 接收用户选择的角色对象
        // 注意：CharacterItem 必须实现 Serializable 接口
        CharacterItem userRole = (CharacterItem) getIntent().getSerializableExtra("USER_ROLE");

        // --- 2. 逻辑处理：告诉 AI 玩家是谁 ---

        if (userRole != null) {
            this.userRoleName = userRole.getName();
            // 🔥【关键技巧】把玩家身份拼接到 Prompt 后面
            // 这样 AI 就知道："哦，原来跟我对话的人是 '大侦探' 啊"
            this.systemPrompt = originalPrompt + "\n\n【当前用户扮演的角色】：" + userRoleName;
        } else {
            this.systemPrompt = originalPrompt;
        }

        // 2. 初始化控件
        initViews(title);

        // 3. 加载历史记录
        loadHistory();
    }

    private void initViews(String title) {
        TextView tvTitle = findViewById(R.id.tv_title);
        ivBack = findViewById(R.id.iv_back);

        tvTitle.setText(title == null ? "剧本杀" : title);

        rvChat = findViewById(R.id.rv_chat);
        etInput = findViewById(R.id.et_input);
        Button btnSend = findViewById(R.id.btn_send);



        // 初始化 Adapter
        adapter = new ChatAdapter();
        String bgStory = getIntent().getStringExtra("BACKGROUND");
        if (bgStory == null) {
            // 你的测试数据
            bgStory = "1883年4月的一个清晨，一位名为海伦·斯托纳的年轻女士惊恐地前来求助。她住在苏里郡的斯托克莫兰庄园，那里住着她性情暴躁、曾去过印度的继父——罗伊洛特医生。\n\n两年前的一个风雨交加的夜晚，她的双胞胎姐姐朱莉亚在自己的房间里离奇死亡。死前房间门窗紧锁，朱莉亚惨叫着冲出房间，最后留下的遗言是：“带子！是带斑点的带子！";

        }
        adapter.setBackgroundStory(bgStory);

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
        DBHelper.loadHistory(this, scriptId, new DataCallback<List<ChatMessage>>() {
            @Override
            public void onSuccess(List<ChatMessage> history) {
                // ✅ 成功拿到数据，更新 UI
                if (history != null && !history.isEmpty()) {
                    adapter.setMessages(history);
                    scrollToBottom();
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                // 数据库查询一般不会失败，这里留空或者打个日志即可
                // Log.e("ChatActivity", "加载历史记录失败: " + errorMessage);
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

        // 5. 🔥 呼叫 AI
        callAI(adapter.getMessages());


    }

    private void scrollToBottom() {
        if (adapter.getItemCount() > 0) {
            rvChat.smoothScrollToPosition(adapter.getItemCount() - 1);
        }
    }

    private void callAI(List<ChatMessage> history) {

        // 调用工具类
        AIUtils.chatWithAI(systemPrompt, history, new DataCallback<String>() {
            @Override
            public void onSuccess(String aiReply) {
                // --- 成功了，逻辑和之前一样 ---
                List<ChatMessage> aiMessages = parseAiResponse(aiReply);
                for (ChatMessage aiMsg : aiMessages) {
                    DBHelper.insertMessage(ChatActivity.this, aiMsg);
                    adapter.addMessage(aiMsg);
                }
                scrollToBottom();
            }

            @Override
            public void onFailure(String errorMessage) {
                // --- 🔥 出错了，在这里处理 ---

                // 方案 A: 简单弹窗 (适合调试)
                // Toast.makeText(ChatActivity.this, errorMessage, Toast.LENGTH_LONG).show();

                // 方案 B: 在聊天窗口显示一条系统警告 (体验更好)
                addSystemMessage("⚠️ " + errorMessage);
            }

            // --- 主线程 ---




        });



    }


    /**
     * 🛠️ 核心工具：把 AI 返回的一大段文本，拆分成多条消息
     * 例如：
     * "你好...\n[管家]: 先生请进"
     * ↓ 拆分成 ↓
     * 1. DM/默认: "你好..."
     * 2. 管家: "先生请进"
     */
    private List<ChatMessage> parseAiResponse(String aiReply) {
        List<ChatMessage> messages = new ArrayList<>();

        // 支持这种格式：
        // [Alice]: xxx
        // [Bob]：xxx
        // 正则含义：捕获 [角色名] 后面跟着 冒号（中英文），并且获取后面的台词
        Pattern pattern = Pattern.compile("\\[(.+?)\\][：:]\\s*");
        Matcher matcher = pattern.matcher(aiReply);

        int lastEnd = 0;
        String lastSpeaker = "系统";

        while (matcher.find()) {
            // ➤ 如果之前有 speaker，保存上一段内容
            if (lastEnd != 0) {
                String content = aiReply.substring(lastEnd, matcher.start()).trim();
                if (!content.isEmpty()) {
                    messages.add(new ChatMessage(scriptId, lastSpeaker, null, content, false));
                }
            }

            // ➤ 更新当前说话人
            lastSpeaker = matcher.group(1).trim();
            lastEnd = matcher.end();
        }

        // ➤ 最后一段内容（如果存在）
        if (lastEnd < aiReply.length()) {
            String content = aiReply.substring(lastEnd).trim();
            if (!content.isEmpty()) {
                messages.add(new ChatMessage(scriptId, lastSpeaker, null, content, false));
            }
        }

        return messages;
    }

    // 辅助方法：添加一条系统提示消息
    private void addSystemMessage(String text) {
        // 这里的 senderName 用 "系统"，isUser=false
        ChatMessage sysMsg = new ChatMessage(scriptId, "系统", null, text, false);

        // 存库 (可选，如果你不想保存报错记录，这行可以删掉)
        DBHelper.insertMessage(this, sysMsg);

        // 显示
        adapter.addMessage(sysMsg);
        scrollToBottom();
    }
}