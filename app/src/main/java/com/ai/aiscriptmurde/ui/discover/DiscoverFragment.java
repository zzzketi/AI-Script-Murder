package com.ai.aiscriptmurde.ui.discover;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ai.aiscriptmurde.R;
import com.ai.aiscriptmurde.db.ChatSessionEntity;
import com.ai.aiscriptmurde.model.ScriptDetailModel;
import com.ai.aiscriptmurde.ui.chat.ChatActivity;
import com.ai.aiscriptmurde.utils.DBHelper;
import com.ai.aiscriptmurde.utils.ScriptUtils;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class DiscoverFragment extends Fragment implements ChatSessionAdapter.OnSessionInteractionListener {

    private RecyclerView recyclerView;
    private ChatSessionAdapter adapter;
    private List<ChatSessionEntity> chatSessions = new ArrayList<>();
    private TextView emptyView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_discover, container, false);
        recyclerView = view.findViewById(R.id.rv_chat_sessions);
        emptyView = view.findViewById(R.id.tv_empty_view);
        setupRecyclerView();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 🔥 核心改造：在这里一次性订阅数据，之后不再需要手动刷新
        loadAndObserveChatSessions();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        adapter = new ChatSessionAdapter(getContext(), chatSessions, this);
        recyclerView.setAdapter(adapter);
    }

    /**
     * 🔥 核心改造：加载并观察来自数据库的实时数据流。
     */
    private void loadAndObserveChatSessions() {
        DBHelper.getAllChatSessions(getContext()).observe(getViewLifecycleOwner(), new Observer<List<ChatSessionEntity>>() {
            @Override
            public void onChanged(List<ChatSessionEntity> sessions) {
                if (sessions != null) {
                    chatSessions.clear();
                    chatSessions.addAll(sessions);
                    adapter.notifyDataSetChanged();
                }
                updateEmptyState();
            }
        });
    }

    private void updateEmptyState() {
        if (chatSessions.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSessionClicked(ChatSessionEntity session) {
        String scriptId = session.getScriptId();
        String fileName = "mock_data/details/script_" + scriptId + ".json";
        String jsonStr = ScriptUtils.readAssetFile(getContext(), fileName);

        if (jsonStr != null) {
            Gson gson = new Gson();
            ScriptDetailModel detail = gson.fromJson(jsonStr, ScriptDetailModel.class);

            if (detail != null) {
                Intent intent = new Intent(getContext(), ChatActivity.class);
                intent.putExtra("SCRIPT_ID", detail.getId());
                intent.putExtra("SCRIPT_TITLE", detail.getTitle());
                intent.putExtra("SYSTEM_PROMPT", detail.getSystemPrompt() != null ? detail.getSystemPrompt() : "");

                String background = "";
                if (detail.getBackground() != null && detail.getBackground().getStory() != null) {
                    background = detail.getBackground().getStory();
                }
                intent.putExtra("BACKGROUND", background);

                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "无法解析剧本详情", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "找不到剧本详情文件: " + fileName, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSessionLongClicked(ChatSessionEntity session) {
        String title = session.getScriptTitle() != null ? session.getScriptTitle() : "此会话";
        new AlertDialog.Builder(getContext())
                .setTitle("删除确认")
                .setMessage("您确定要永久删除 ‘" + title + "’ 的所有聊天记录吗？此操作不可撤销。")
                .setPositiveButton("删除", (dialog, which) -> {
                    DBHelper.deleteChatHistory(getContext(), session.getScriptId(), () -> {
                        // No need to manually remove from list, LiveData will do it automatically.
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }
}