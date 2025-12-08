package com.ai.aiscriptmurde.ui.discover;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import com.ai.aiscriptmurde.model.CharacterItem;
import com.ai.aiscriptmurde.model.ScriptDetailModel;
import com.ai.aiscriptmurde.network.RetrofitClient;
import com.ai.aiscriptmurde.ui.chat.ChatActivity;
import com.ai.aiscriptmurde.utils.DBHelper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
        // 直接调用网络请求方法，进去之后会自动处理跳转
        fetchScriptAndEnterChat(getContext(), session.getScriptId(), session);
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

    // 在你的 Fragment 或 Adapter 或 Activity 中

    private void fetchScriptAndEnterChat(Context context, String scriptId, ChatSessionEntity session) {

        // 1. 【体验优化】显示 Loading，因为网络请求需要时间
//        ProgressDialog loadingDialog = new ProgressDialog(context);
//        loadingDialog.setMessage("正在获取剧本数据...");
//        loadingDialog.setCancelable(false);
//        loadingDialog.show();

        // 2. 发起网络请求
        RetrofitClient.getApiService().getScriptDetail(scriptId).enqueue(new Callback<ScriptDetailModel>() {
            @Override
            public void onResponse(Call<ScriptDetailModel> call, Response<ScriptDetailModel> response) {
//                loadingDialog.dismiss(); // 关闭 Loading

                if (response.isSuccessful() && response.body() != null) {
                    // 3. 【核心】数据拿到了！在这里组装 Intent 并跳转
                    ScriptDetailModel detail = response.body();
                    enterChatActivity(context, detail, session);

                } else {
                    Toast.makeText(context, "加载剧本失败: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ScriptDetailModel> call, Throwable t) {
//                loadingDialog.dismiss(); // 关闭 Loading
                Toast.makeText(context, "网络错误，请检查连接", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 辅助方法：执行跳转逻辑
    private void enterChatActivity(Context context, ScriptDetailModel detail, ChatSessionEntity session) {
        // 1. 尝试从 SP 读取“我的角色”信息 (用于续玩)
        SharedPreferences sp = context.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE);
        String myRoleId = sp.getString("my_role_id_" + detail.getId(), null);
        String myRoleName = sp.getString("my_role_name_" + detail.getId(), "");
        String myRoleAvatar = sp.getString("my_role_avatar_" + detail.getId(), "");

        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra("SCRIPT_ID", detail.getId());
        intent.putExtra("SCRIPT_TITLE", detail.getTitle());

        // 2. 传递完整的角色列表 (用于显示头像)
        // 因为 detail 是刚从网络拉下来的，里面肯定有 characters
        if (detail.getCharacters() != null) {
            intent.putExtra("ALL_CHARACTERS", (Serializable) detail.getCharacters());
        }

        // 3. 恢复“我的角色”
        if (myRoleId != null) {
            CharacterItem myRole = new CharacterItem();
            myRole.setId(myRoleId);
            myRole.setName(myRoleName);
            myRole.setAvatar(myRoleAvatar);
            intent.putExtra("USER_ROLE", myRole);
        } else {
            // 如果 SP 里没有角色信息，说明数据丢了，或者这是新开的局
            // 这种情况下，通常应该跳到“选角页面”，或者默认取第一个角色（如果是测试）
            //todo
            //intent.putExtra("USER_ROLE", detail.getCharacters().get(0));
        }

        context.startActivity(intent);
    }


}