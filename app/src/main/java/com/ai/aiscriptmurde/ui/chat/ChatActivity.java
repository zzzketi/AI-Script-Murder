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
import com.ai.aiscriptmurde.utils.AIUtils;
import com.ai.aiscriptmurde.utils.DBHelper;
import com.ai.aiscriptmurde.utils.DataCallback;

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
        systemPrompt = "你现在是剧本杀主持人。当前剧本改编自《斑点带子案》。\n\n【玩家身份】：大侦探（正在勘查案发现场）。\n\n【你需要扮演的NPC】：\n1. 海伦·斯托纳（死者的妹妹，委托人）：性格柔弱惊恐，非常害怕继父。她因为房间装修被迫搬进了姐姐死去的房间，昨晚听到了姐姐死前听到的口哨声。\n2. 罗伊洛特医生（继父，凶手）：凶狠暴躁，身材高大，去过印度，养着狒狒和猎豹。非常反感侦探的调查。\n\n【核心真相（仅AI可见）】：\n- 凶手是继父罗伊洛特医生。\n- 动机：如果女儿出嫁，他掌管的遗产就会减少。两年前姐姐朱莉亚要结婚，所以被杀；现在海伦也要结婚，所以他故技重施。\n- 凶器：一条来自印度的沼泽蝰蛇（斑点带子）。\n- 手法：他训练蛇通过通气孔爬进隔壁房间，顺着床边的铃绳爬下去咬人。听到口哨声后，蛇会爬回来喝牛奶。\n\n【关键线索（玩家问到时必须透露）】：\n1. 房间结构：床被钉死在地板上无法移动；通气孔不通向室外，而是通向继父的房间。\n2. 铃绳：看起来是新的，但没有连接任何铃铛，只挂在通气孔挂钩上。\n3. 继父房间：有一个保险柜（里面关着蛇），一盘牛奶，一把类似狗鞭的鞭子。\n4. 死亡遗言：姐姐死前喊的是“斑点带子”，海伦以为是吉普赛人的头巾，其实是蛇身上的花纹。\n\n【回复规则】：\n- 请以群聊形式回复，格式为“[角色名]: 内容”。\n- 继父面对质问要表现出愤怒和威胁。\n- 海伦对继父非常恐惧，只敢在继父不在时多说话。";
        String title = getIntent().getStringExtra("SCRIPT_TITLE");

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
        callAI(content);


    }

    private void scrollToBottom() {
        if (adapter.getItemCount() > 0) {
            rvChat.smoothScrollToPosition(adapter.getItemCount() - 1);
        }
    }

    private void callAI(String userContent) {

        // 调用工具类
        AIUtils.chatWithAI(systemPrompt, userContent, new DataCallback<String>() {
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
        List<ChatMessage> resultMessages = new ArrayList<>();

        // 按行切割，逐行分析
        String[] lines = aiReply.split("\n");

        // 默认的第一说话人（如果第一句没写名字，就假设是旁白或上一轮的角色）
        // 你可以根据需要改成 "DM" 或者 "系统"
        String currentSender = "系统";
        StringBuilder currentContent = new StringBuilder();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue; // 跳过空行

            // 🔍 判断这一行是不是新角色发言
            // 特征：以 '[' 开头，并且包含 ']:' 或 ']：'
            boolean isNewRole = line.startsWith("[") && (line.contains("]:") || line.contains("]："));

            if (isNewRole) {
                // 1. 如果之前缓冲区里有内容，先打包上一条消息
                if (currentContent.length() > 0) {
                    resultMessages.add(new ChatMessage(scriptId, currentSender, null, currentContent.toString(), false));
                    currentContent.setLength(0); // 清空缓冲区
                }

                // 2. 提取新名字
                try {
                    // 兼容英文冒号和中文冒号
                    int splitIndex = line.contains("]:") ? line.indexOf("]:") : line.indexOf("]：");
                    currentSender = line.substring(0, splitIndex).replace("[", "").replace("]", "");

                    // 3. 把这一行剩下的内容作为新内容的开始
                    // +2 是跳过 "]:" 两个字符
                    String content = line.substring(splitIndex + 2).trim();
                    currentContent.append(content);
                } catch (Exception e) {
                    // 解析失败就当做普通文本追加
                    currentContent.append(line);
                }
            } else {
                // 不是新角色，说明是上一句话的换行（或者是第一句话）
                if (currentContent.length() > 0) {
                    currentContent.append("\n"); // 补回换行符
                }
                currentContent.append(line);
            }
        }

        // 4. 循环结束，别忘了把最后一段也没存进去
        if (currentContent.length() > 0) {
            resultMessages.add(new ChatMessage(scriptId, currentSender, null, currentContent.toString(), false));
        }

        return resultMessages;
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