package com.ai.aiscriptmurde.ui.chat;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;


import com.ai.aiscriptmurde.R;
import com.ai.aiscriptmurde.db.ChatMessage;
import com.ai.aiscriptmurde.model.CharacterItem;
import com.ai.aiscriptmurde.model.CreateSessionRequest;
import com.ai.aiscriptmurde.model.MessageRequest;
import com.ai.aiscriptmurde.model.NextChapterResponse;
import com.ai.aiscriptmurde.model.SessionResponse;
import com.ai.aiscriptmurde.network.RetrofitClient;
import com.ai.aiscriptmurde.network.StreamCallback;
import com.ai.aiscriptmurde.network.StreamManager;
import com.ai.aiscriptmurde.network.StreamUiCallback;
import com.ai.aiscriptmurde.utils.MultiRoleStreamHandler;
import com.ai.aiscriptmurde.utils.RolePlayStreamHandler;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    // --- UI 组件 ---
    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private EditText etInput;
    private View btnSend; // 可以是 ImageView 或 FloatingActionButton
    private Toolbar toolbar;
    private ImageView btnReadScript;

    private TextView tvTitle;
    private TextView tvSubtitle;


    private MaterialButton btnNextStage;
    private String currentScriptNarration = "";
    private String currentChapterTitle = "序章";

    // --- 数据变量 ---
    private List<ChatMessage> messageList = new ArrayList<>();
    private String scriptId; // 假设从上个页面传过来
    private String sessionId;                // 后端返回的会话ID

    // --- 游戏状态控制 ---
    private int currentChapterIndex = 0;     // 当前章节: 0=未开始, 1-5=游戏中
    private final int MAX_CHAPTERS = 5;      // 最大章节数
    private boolean isGameEnded = false;     // 游戏是否结束
    // 【新增】保存当前用户的角色对象
    private CharacterItem currentUserRole;



    Serializable userRoleSerializable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //传参
//        scriptId = getIntent().getStringExtra("EXTRA_SCRIPT_ID");
//        userRoleId = getIntent().getStringExtra("EXTRA_ROLE_ID");
        retrieveIntentData();

        // 1. 初始化视图
        initViews();

        // 2. 初始化列表适配器
        initRecyclerView();

        // 3. 绑定点击事件
        initListeners();



        // 4. 自动开始游戏 (获取 Session)
        startNewGameSession();
    }

    private void retrieveIntentData() {
        userRoleSerializable = getIntent().getSerializableExtra("USER_ROLE");
        // 获取 ScriptID (假设你也传了这个)
        if (getIntent().hasExtra("SCRIPT_ID")) {
            scriptId = getIntent().getStringExtra("SCRIPT_ID");
        }

        // 【关键】获取 Serializable 对象
        // 注意：Android 13 (API 33) 对 getSerializableExtra 做了变更，为了兼容性建议做个判断
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            currentUserRole = getIntent().getSerializableExtra("USER_ROLE", CharacterItem.class);
        } else {
            // 旧版本写法
            currentUserRole = (CharacterItem) getIntent().getSerializableExtra("USER_ROLE");
        }

        if (currentUserRole == null) {
            // --- 调试模式开始 ---
            Toast.makeText(this, "⚠️ 测试模式：使用模拟角色数据", Toast.LENGTH_SHORT).show();

            // 创建一个假的 CharacterItem
            currentUserRole = new CharacterItem("c_detective","福尔摩斯 (测试)","https://example.com/avatar.png","23","我是来测试的侦探","介绍");



            // 给 scriptId 也赋个默认值
            if (scriptId == null) scriptId = "script_1";
            // --- 调试模式结束 ---

            // ❌ 删掉这行，不要 finish！
            // finish();
        } else {
            // 正常获取到了数据
            if (getSupportActionBar() != null) {
                getSupportActionBar().setSubtitle("扮演: " + currentUserRole.getName());
            }
        }
    }

    private void initViews() {
        btnReadScript = findViewById(R.id.btnReadScript);
        toolbar = findViewById(R.id.toolbar);
        btnNextStage = findViewById(R.id.btnNextStage);
        setSupportActionBar(toolbar); // 【关键】设置 Toolbar 才能显示菜单
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);

            // 如果你需要左上角的返回箭头，把下面这句也加上：
            // getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        tvTitle = findViewById(R.id.tvScriptTitle);
        tvSubtitle = findViewById(R.id.tvChapterName); // 或者是 tvChapterTag

        //从获取的那里获得
        tvTitle.setText("新的剧本名称");
        tvSubtitle.setText("CHAPTER 0 · 序章");

        recyclerView = findViewById(R.id.recyclerView);
        etInput = findViewById(R.id.etInput);
        btnSend = findViewById(R.id.btnSend);
    }

    private void initRecyclerView() {
        // 注意：Adapter 构造函数不需要 Context，我们在 Adapter 内部获取
        chatAdapter = new ChatAdapter(messageList);

        // 【新增】关闭默认的更新动画，解决流式输出时的闪烁问题
        if (recyclerView.getItemAnimator() instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        // 保持软键盘弹出时列表顶上去，而不是被遮挡
        layoutManager.setStackFromEnd(false);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(chatAdapter);
    }

    private void initListeners() {
        btnReadScript.setOnClickListener(v -> {
            showScriptDialog();
        });
        btnNextStage.setOnClickListener(v -> {
            handleStageAction();

            if (currentChapterIndex >= MAX_CHAPTERS) {
                btnNextStage.setText("投票"); // 最后一章显示投票
            } else {
                btnNextStage.setText("进入下一章>"); // 平时显示下一章
            }

            // 3. 处理显示/隐藏
            // 如果游戏结束，隐藏(GONE)；没结束，显示(VISIBLE)
            if (isGameEnded) {
                btnNextStage.setVisibility(View.GONE);
            } else {
                btnNextStage.setVisibility(View.VISIBLE);
            }
        });

        // 发送按钮点击事件
        btnSend.setOnClickListener(v -> {
            String content = etInput.getText().toString().trim();
            if (!TextUtils.isEmpty(content)) {
                sendMessageToNpc(content);
                etInput.setText(""); // 清空输入框
            }


        });



    }




    private void showScriptDialog() {
        // 如果没有内容，提示一下
        if (TextUtils.isEmpty(currentScriptNarration)) {
            Toast.makeText(this, "当前没有剧本内容可阅读", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. 创建 BottomSheetDialog
        BottomSheetDialog dialog = new BottomSheetDialog(this);

        // 2. 加载布局 (我们可以动态创建，也可以写一个简单的 layout xml)
        // 为了代码简洁，这里演示动态创建 View，你也可以去写个 R.layout.dialog_script_read
        View view = getLayoutInflater().inflate(R.layout.dialog_read_script, null);

        // 3. 绑定数据
        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        TextView tvContent = view.findViewById(R.id.tvDialogContent);
        ImageView btnClose = view.findViewById(R.id.btnClose);

        tvTitle.setText(currentChapterTitle);
        tvContent.setText(currentScriptNarration);

        // 4. 关闭事件
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // 5. 显示
        dialog.setContentView(view);
        dialog.show();
    }

    // ================================================================
    //   Toolbar Menu 逻辑 (右上角的 搜索 & 下一章)
    // ================================================================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 加载我们在 res/menu/menu_chat.xml 定义的菜单
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_search) {
            // 处理搜索逻辑
            Toast.makeText(this, "打开线索搜证面板...", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ================================================================
    //   核心游戏业务逻辑 (API 调用模拟)
    // ================================================================

    /**
     * 逻辑分发：是进入下一章，还是发起投票？
     */
    private void handleStageAction() {
        if (currentChapterIndex < MAX_CHAPTERS) {
            // 还有章节，加载下一章
            loadNextChapter();
        } else {
            // 已经是最后一章，发起投票
            showVoteDialog();
        }
    }

    /**
     * 1. API: POST /sessions
     * 创建游戏会话
     */
    private void startNewGameSession() {
        //这里要替换掉：currentUserRole.getId()
        // 1. 构建请求对象
        CreateSessionRequest requestBody = new CreateSessionRequest(
                "script_1",
                "c_detective",
                "Qwen/Qwen2.5-72B-Instruct"
        );

        RetrofitClient.getApiService().createSession(requestBody).enqueue(new Callback<SessionResponse>() {

            @Override
            public void onResponse(Call<SessionResponse> call, Response<SessionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Retrofit 已经帮你把 JSON 转成了 SessionResponse 对象
                    SessionResponse data = response.body();
                    sessionId = data.getSessionId();

                    runOnUiThread(() -> {
                        Toast.makeText(ChatActivity.this, "开局成功", Toast.LENGTH_SHORT).show();
                        // 拿到 ID 后，加载下一章
                        loadNextChapter();
                    });
                } else {
                    runOnUiThread(() -> {
                        setLoadingState(false);
                        Toast.makeText(ChatActivity.this, "创建失败: " + response.code(), Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onFailure(Call<SessionResponse> call, Throwable t) {
                runOnUiThread(() -> {
                    setLoadingState(false);
                    Toast.makeText(ChatActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * 2. API: POST /sessions/{id}/next_chapter
     * 获取章节剧情 + 开放性问题
     */
    private void loadNextChapter() {
        if (sessionId == null) return;

        // 1. 锁定界面，防止重复点击
        setLoadingState(true);

        // 2. 发起网络请求
        RetrofitClient.getApiService().triggerNextChapter(sessionId).enqueue(new Callback<NextChapterResponse>() {
            @Override
            public void onResponse(Call<NextChapterResponse> call, Response<NextChapterResponse> response) {
                // 无论成功失败，都要解锁界面 (放在 finally 或者两边都写)
                setLoadingState(false);

                if (response.isSuccessful() && response.body() != null) {
                    NextChapterResponse data = response.body();

                    // --- A. 处理数据同步 ---
                    currentChapterIndex = data.getChapterIndex();

                    if (data.getNarration() != null) {
                        currentScriptNarration = data.getNarration();
                    } else {
                        currentScriptNarration = "当前章节无额外剧本内容。";
                    }

                    // --- B. 更新 Toolbar 标题 ---
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setSubtitle("第 " + currentChapterIndex + " 章");
                    }
                    tvSubtitle.setText("CHAPTER "+currentChapterIndex+"· 第" + currentChapterIndex + " 章");

                    // --- C. 构建并插入 UI 消息 ---



                    // 2. 开放性问题 (系统提示 / System Message)
                    if (data.getDiscussionQuestion() != null && !data.getDiscussionQuestion().isEmpty()) {
                        ChatMessage systemMsg = new ChatMessage(
                                 data.getDiscussionQuestion(),
                                ChatMessage.TYPE_SYSTEM
                        );
                        addMessageToChat(systemMsg);
                    }

                    // --- D. 检查游戏状态 (是否进入投票环节) ---
                    // 假设后端 status 返回 "voting" 代表结局
                    if ("voting".equalsIgnoreCase(data.getStatus()) || currentChapterIndex >= MAX_CHAPTERS) {
                        // 标记游戏可能即将结束，刷新右上角菜单显示 "投票"
                        invalidateOptionsMenu();
                    }

                } else {
                    Toast.makeText(ChatActivity.this, "剧情加载失败: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<NextChapterResponse> call, Throwable t) {
                setLoadingState(false);
                Toast.makeText(ChatActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 3. API: POST /sessions/{id}/message
     * 用户发送消息 -> 获取 AI 回复
     */
    private void sendMessageToNpc(String text) {
        // 1. UI 显示用户消息
        addMessageToChat(new ChatMessage(text, ChatMessage.TYPE_USER));


        // 2. 发起请求
        MessageRequest request = new MessageRequest(text);
        Call<ResponseBody> call = RetrofitClient.getApiService().sendMessageStream(sessionId, request);

        // 3. 使用新的 Handler
        MultiRoleStreamHandler.handle(call, new MultiRoleStreamHandler.MultiRoleCallback() {

            // 记录当前正在更新的那条消息
            private ChatMessage currentStreamingMsg = null;

            @Override
            public void onSwitchRole(String roleName) {
                // 【关键】检测到新角色，创建新气泡
                currentStreamingMsg = new ChatMessage("", ChatMessage.TYPE_PLOT);
                currentStreamingMsg.setSenderName(roleName);

                // 模拟头像：实际开发中你可以根据 roleName 去查找头像 URL
                // currentStreamingMsg.setAvatarUrl(findAvatarByName(roleName));

                addMessageToChat(currentStreamingMsg);
            }

            @Override
            public void onAppendContent(String content) {
                if (currentStreamingMsg == null) {
                    // 如果还没检测到角色名就来了内容（比如旁白，或者第一句话没带名字）
                    // 我们可以创建一个默认的“旁白”或者“系统”消息
                    currentStreamingMsg = new ChatMessage("", ChatMessage.TYPE_PLOT); // 或者 TYPE_NPC
                    currentStreamingMsg.setSenderName("旁白");
                    addMessageToChat(currentStreamingMsg);
                }

                // 拼接内容
                String oldContent = currentStreamingMsg.getContent();
                currentStreamingMsg.setContent(oldContent + content);

                // 局部刷新 (使用 Payload 防止头像闪烁)
                chatAdapter.notifyItemChanged(chatAdapter.getItemCount() - 1, "UPDATE_TEXT");
                recyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
            }

            @Override
            public void onComplete() {
                setLoadingState(false);
            }

            @Override
            public void onError(Throwable t) {
                setLoadingState(false);
                Toast.makeText(ChatActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }




    /**
     * 4. API: POST /sessions/{id}/vote
     * 投票并结束游戏
     */
    private void showVoteDialog() {
        // 弹窗让用户选择凶手
        final String[] suspects = {"管家", "女仆", "医生", "律师"};

        new AlertDialog.Builder(this)
                .setTitle("指认凶手")
                .setSingleChoiceItems(suspects, -1, (dialog, which) -> {
                    // 选中后的逻辑
                })
                .setPositiveButton("确定投票", (dialog, which) -> {
                    // 发起投票 API
                    submitVote(suspects[0]); // 这里取选中的值
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void submitVote(String suspect) {
        // ApiClient.postVote(...)
        // 模拟回调
        ChatMessage endMsg = new ChatMessage(
                "结局揭晓："+ "你指认了 " + suspect + "。真相是...",
                ChatMessage.TYPE_SYSTEM
        );
        addMessageToChat(endMsg);

        // 游戏结束，禁用输入
        setInputEnabled(false);
        btnNextStage.setVisibility(View.GONE);
        etInput.setHint("游戏已结束");
    }

    // ==========================================
    //              辅助 UI 方法
    // ==========================================

    private void addMessageToChat(ChatMessage msg) {
        chatAdapter.addMessage(msg); // 假设你在 Adapter 里写了这个方法
        // 滚动到底部
        recyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
    }

    private void setInputEnabled(boolean enabled) {
        etInput.setEnabled(enabled);
        btnSend.setEnabled(enabled);
        btnNextStage.setEnabled(enabled);
    }

    private void updateUIState() {
        tvSubtitle.setText("CHAPTER "+currentChapterIndex+"· 第" + currentChapterIndex + " 章");

        if (currentChapterIndex >= MAX_CHAPTERS) {
            btnNextStage.setText("发起投票 (结局)");
            btnNextStage.setIconResource(android.R.drawable.ic_lock_power_off); // 换个图标
        } else {
            btnNextStage.setText("进入下一章");
        }
    }

    /**
     * 控制 UI 的加载状态
     * @param isLoading
     * true  = 正在请求网络（禁用按钮，禁止输入）
     * false = 请求结束（恢复按钮，允许输入）
     */
    private void setLoadingState(boolean isLoading) {
        // 1. 发送按钮：加载时不可点，变灰
        if (btnSend != null) {
            btnSend.setEnabled(!isLoading);
        }

        // 2. 输入框：加载时禁止输入，防止用户在 AI 回复时乱打字
        // 注意：如果游戏已经结束了(isGameEnded)，就永远不再启用输入框
        if (etInput != null) {
            etInput.setEnabled(!isLoading && !isGameEnded);
        }

        // 3. 右上角的菜单（下一章/投票）：加载时通常也要禁用，防止逻辑冲突
        // (这需要你把 menu item 存为变量，或者在 invalidateOptionsMenu 里处理)

        // 4. (可选) 显示/隐藏 进度条
        // if (progressBar != null) {
        //     progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        // }
    }
}