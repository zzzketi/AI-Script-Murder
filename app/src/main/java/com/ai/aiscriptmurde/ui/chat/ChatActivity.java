package com.ai.aiscriptmurde.ui.chat;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatActivity extends AppCompatActivity {

    private static final int SEARCH_REQUEST_CODE = 101;

    private RecyclerView rvChat;
    private ChatAdapter adapter;
    private EditText etInput;
    private TextView tvTitle;
    private ImageView ivBack;
    private ImageView ivSearch;
    private ImageView ivMore;
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
        loadDataAndScroll(getIntent());
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (scriptId != null) {
            DBHelper.clearUnreadCount(this, scriptId);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        loadDataAndScroll(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SEARCH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            long timestamp = data.getLongExtra(ChatSearchActivity.RESULT_TIMESTAMP, -1);
            String content = data.getStringExtra(ChatSearchActivity.RESULT_CONTENT);
            if (timestamp != -1 && content != null) {
                highlightMessage(timestamp, content);
            }
        }
    }

    private void initViews(String title) {
        tvTitle = findViewById(R.id.tv_title);
        ivBack = findViewById(R.id.iv_back);
        ivSearch = findViewById(R.id.iv_search);
        ivMore = findViewById(R.id.iv_more);
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
        ivSearch.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatSearchActivity.class);
            intent.putExtra(ChatSearchActivity.EXTRA_SCRIPT_ID, scriptId);
            startActivityForResult(intent, SEARCH_REQUEST_CODE);
        });

        // 🔥 新增：为“更多”按钮添加点击事件
        ivMore.setOnClickListener(this::showPopupMenu);

        btnSend.setOnClickListener(v -> {
            String content = etInput.getText().toString().trim();
            if (TextUtils.isEmpty(content)) {
                Toast.makeText(this, "不能发送空消息哦", Toast.LENGTH_SHORT).show();
                return;
            }
            sendMessage(content);
        });
    }

    private void showPopupMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.getMenuInflater().inflate(R.menu.chat_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_clear_history) {
                showClearHistoryConfirmationDialog();
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void showClearHistoryConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("清空聊天记录")
                .setMessage("您确定要清空当前聊天记录吗？此操作不可撤销。")
                .setPositiveButton("清空", (dialog, which) -> {
                    DBHelper.clearChatMessages(ChatActivity.this, scriptId, () -> {
                        adapter.setMessages(new ArrayList<>());
                        Toast.makeText(ChatActivity.this, "聊天记录已清空", Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void loadDataAndScroll(Intent intent) {
        long highlightTimestamp = intent.getLongExtra(ChatSearchActivity.RESULT_TIMESTAMP, -1);
        String highlightContent = intent.getStringExtra(ChatSearchActivity.RESULT_CONTENT);

        DBHelper.getSessionAndCreateIfNotExist(this, scriptId, scriptTitle, new DataCallback<ChatSessionEntity>() {
            @Override
            public void onSuccess(ChatSessionEntity session) {
                if (highlightTimestamp != -1 && highlightContent != null) {
                    loadHistory(0, highlightTimestamp, highlightContent);
                } else {
                    loadHistory(session.getUnreadCount(), -1, null);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                loadHistory(0, -1, null);
            }
        });
    }

    private void loadHistory(int unreadCount, long highlightTimestamp, String highlightContent) {
        DBHelper.loadHistory(this, scriptId, new DataCallback<List<ChatMessage>>() {
            @Override
            public void onSuccess(List<ChatMessage> history) {
                if (history != null) { // Allow empty history
                    adapter.setMessages(history);

                    if (highlightTimestamp != -1 && highlightContent != null) {
                        highlightMessage(highlightTimestamp, highlightContent);
                    } else if (unreadCount > 0 && unreadCount <= history.size()) {
                        LinearLayoutManager layoutManager = (LinearLayoutManager) rvChat.getLayoutManager();
                        if (layoutManager != null) {
                            int position = adapter.findPositionByTimestampAndContent(history.get(history.size() - unreadCount).getTimestamp(), history.get(history.size() - unreadCount).getContent());
                            if (position != -1) {
                                layoutManager.scrollToPositionWithOffset(position, 0);
                            }
                        }
                    } else if (!history.isEmpty()){
                        rvChat.scrollToPosition(adapter.getItemCount() - 1);
                    }
                }
            }

            @Override
            public void onFailure(String errorMessage) { }
        });
    }

    private void highlightMessage(long timestamp, String content) {
        int position = adapter.findPositionByTimestampAndContent(timestamp, content);
        if (position != -1) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) rvChat.getLayoutManager();
            if (layoutManager != null) {
                layoutManager.scrollToPositionWithOffset(position, 0);
                rvChat.post(() -> {
                    RecyclerView.ViewHolder holder = rvChat.findViewHolderForAdapterPosition(position);
                    if (holder != null) {
                        final View itemView = holder.itemView;
                        itemView.setBackgroundColor(ContextCompat.getColor(ChatActivity.this, R.color.highlight_color));
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            itemView.setBackgroundColor(ContextCompat.getColor(ChatActivity.this, android.R.color.transparent));
                        }, 1000);
                    }
                });
            }
        }
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
                    adapter.addMessage(aiMsg);
                    DBHelper.insertMessage(ChatActivity.this, aiMsg);
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
        adapter.addMessage(sysMsg);
        DBHelper.insertMessage(this, sysMsg);
        scrollToBottom();
    }
}