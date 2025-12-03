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
import com.ai.aiscriptmurde.db.ChatMessage;
import com.ai.aiscriptmurde.model.CharacterItem; // 🔥 修复：改回使用 CharacterItem
import com.ai.aiscriptmurde.utils.AIUtils;
import com.ai.aiscriptmurde.utils.DBHelper;
import com.ai.aiscriptmurde.utils.DataCallback;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvChat;
    private ChatAdapter adapter;
    private EditText etInput;
    private TextView tvTitle;
    private ImageView ivBack;
    private Button btnSend;

    private String scriptId;
    private String scriptTitle;
    private String systemPrompt;
    private String userRoleName = "玩家";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        scriptId = getIntent().getStringExtra("SCRIPT_ID");
        scriptTitle = getIntent().getStringExtra("SCRIPT_TITLE");

        if (scriptId == null) scriptId = "default_id";
        if (scriptTitle == null) scriptTitle = "剧本杀";

        DBHelper.createSessionIfNotExists(this, scriptId, scriptTitle);

        String originalPrompt = getIntent().getStringExtra("SYSTEM_PROMPT");
        if (originalPrompt == null) originalPrompt = "你是剧本杀主持人。";

        Serializable userRoleSerializable = getIntent().getSerializableExtra("USER_ROLE");
        // 🔥 修复：检查和转换的类型改回 CharacterItem
        if (userRoleSerializable instanceof CharacterItem) {
            CharacterItem userRole = (CharacterItem) userRoleSerializable;
            this.userRoleName = userRole.getName();
            this.systemPrompt = originalPrompt + "\n\n【当前用户扮演的角色】：“ + userRoleName";
        } else {
            this.systemPrompt = originalPrompt;
        }

        initViews(scriptTitle);
        loadHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (scriptId != null) {
            DBHelper.clearUnreadCount(this, scriptId);
        }
    }

    private void initViews(String title) {
        tvTitle = findViewById(R.id.tv_title);
        ivBack = findViewById(R.id.iv_back);
        tvTitle.setText(title);

        rvChat = findViewById(R.id.rv_chat);
        etInput = findViewById(R.id.et_input);
        btnSend = findViewById(R.id.btn_send);

        adapter = new ChatAdapter();
        String bgStory = getIntent().getStringExtra("BACKGROUND");
        if (bgStory != null) {
            adapter.setBackgroundStory(bgStory);
        }

        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(adapter);

        ivBack.setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> {
            String content = etInput.getText().toString().trim();
            if (TextUtils.isEmpty(content)) {
                Toast.makeText(this, "不能发送空消息哦", Toast.LENGTH_SHORT).show();
                return;
            }
            sendMessage(content);
        });
    }

    private void loadHistory() {
        DBHelper.loadHistory(this, scriptId, new DataCallback<List<ChatMessage>>() {
            @Override
            public void onSuccess(List<ChatMessage> history) {
                if (history != null && !history.isEmpty()) {
                    adapter.setMessages(history);
                    scrollToBottom();
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                // Log error
            }
        });
    }

    private void sendMessage(String content) {
        etInput.setText("");
        ChatMessage userMsg = new ChatMessage(scriptId, userRoleName, null, content, true);
        adapter.addMessage(userMsg);
        scrollToBottom();
        DBHelper.insertMessage(this, userMsg);
        callAI(adapter.getMessages());
    }

    private void scrollToBottom() {
        if (adapter.getItemCount() > 0) {
            rvChat.smoothScrollToPosition(adapter.getItemCount() - 1);
        }
    }

    private void callAI(List<ChatMessage> history) {
        AIUtils.chatWithAI(systemPrompt, history, new DataCallback<String>() {
            @Override
            public void onSuccess(String aiReply) {
                List<ChatMessage> aiMessages = parseAiResponse(aiReply);
                for (ChatMessage aiMsg : aiMessages) {
                    DBHelper.insertMessage(ChatActivity.this, aiMsg);
                    adapter.addMessage(aiMsg);
                }
                scrollToBottom();
            }

            @Override
            public void onFailure(String errorMessage) {
                addSystemMessage("⚠️ " + errorMessage);
            }
        });
    }

    private List<ChatMessage> parseAiResponse(String aiReply) {
        List<ChatMessage> messages = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\[(.+?)\\][：:]\\s*");
        Matcher matcher = pattern.matcher(aiReply);
        int lastEnd = 0;
        String lastSpeaker = "系统";

        while (matcher.find()) {
            if (lastEnd != 0) {
                String content = aiReply.substring(lastEnd, matcher.start()).trim();
                if (!content.isEmpty()) {
                    messages.add(new ChatMessage(scriptId, lastSpeaker, null, content, false));
                }
            }
            lastSpeaker = matcher.group(1).trim();
            lastEnd = matcher.end();
        }

        if (lastEnd < aiReply.length()) {
            String content = aiReply.substring(lastEnd).trim();
            if (!content.isEmpty()) {
                messages.add(new ChatMessage(scriptId, lastSpeaker, null, content, false));
            }
        }
        return messages;
    }

    private void addSystemMessage(String text) {
        ChatMessage sysMsg = new ChatMessage(scriptId, "系统", null, text, false);
        DBHelper.insertMessage(this, sysMsg);
        adapter.addMessage(sysMsg);
        scrollToBottom();
    }
}