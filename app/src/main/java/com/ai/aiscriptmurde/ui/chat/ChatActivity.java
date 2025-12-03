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
import com.ai.aiscriptmurde.db.ChatSessionEntity;
import com.ai.aiscriptmurde.model.CharacterItem;
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
        if (userRoleSerializable instanceof CharacterItem) {
            CharacterItem userRole = (CharacterItem) userRoleSerializable;
            this.userRoleName = userRole.getName();
            this.systemPrompt = originalPrompt + "\n\n【当前用户扮演的角色】:" + userRoleName;
        } else {
            this.systemPrompt = originalPrompt;
        }

        initViews(scriptTitle);
        loadDataAndScroll();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 🔥 修复：移除这里的 clearUnreadCount 调用，以解决竞争问题
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
    
    private void loadDataAndScroll() {
        DBHelper.getSession(this, scriptId, new DataCallback<ChatSessionEntity>() {
            @Override
            public void onSuccess(ChatSessionEntity session) {
                final int unreadCount = session.getUnreadCount();
                
                // 🔥 修复：在拿到未读数后，立即清空数据库中的计数
                if (unreadCount > 0) {
                    DBHelper.clearUnreadCount(ChatActivity.this, scriptId);
                }
                
                loadHistory(unreadCount);
            }

            @Override
            public void onFailure(String errorMessage) {
                loadHistory(0);
            }
        });
    }

    private void loadHistory(int unreadCount) {
        DBHelper.loadHistory(this, scriptId, new DataCallback<List<ChatMessage>>() {
            @Override
            public void onSuccess(List<ChatMessage> history) {
                if (history != null && !history.isEmpty()) {
                    adapter.setMessages(history);
                    
                    if (unreadCount > 0 && unreadCount <= history.size()) {
                        // 🔥 优化：使用更精确的滚动方法，确保第一条未读消息对齐到顶部
                        LinearLayoutManager layoutManager = (LinearLayoutManager) rvChat.getLayoutManager();
                        if (layoutManager != null) {
                            layoutManager.scrollToPositionWithOffset(history.size() - unreadCount, 0);
                        }
                    } else {
                        rvChat.scrollToPosition(history.size() - 1);
                    }
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