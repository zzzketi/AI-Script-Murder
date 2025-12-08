package com.ai.aiscriptmurde.ui.chat;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
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
import com.ai.aiscriptmurde.db.ChatSessionEntity;
import com.ai.aiscriptmurde.model.CharacterItem;
import com.ai.aiscriptmurde.model.CreateSessionRequest;
import com.ai.aiscriptmurde.model.MessageRequest;
import com.ai.aiscriptmurde.model.NextChapterResponse;
import com.ai.aiscriptmurde.model.ScriptDetailModel;
import com.ai.aiscriptmurde.model.SessionResponse;
import com.ai.aiscriptmurde.model.VoteRequest;
import com.ai.aiscriptmurde.model.VoteResultModel;
import com.ai.aiscriptmurde.network.RetrofitClient;
import com.ai.aiscriptmurde.network.StreamCallback;
import com.ai.aiscriptmurde.network.StreamManager;
import com.ai.aiscriptmurde.network.StreamUiCallback;
import com.ai.aiscriptmurde.ui.scriptlist.ScriptDetailActivity;
import com.ai.aiscriptmurde.utils.DBHelper;
import com.ai.aiscriptmurde.utils.DataCallback;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private String scriptTitle;

    private List<CharacterItem> allCharacters;

    private Map<String, CharacterItem> characterMap = new HashMap<>();



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


        retrieveIntentData();

        // 1. 初始化视图
        initViews();

        // 2. 初始化列表适配器
        initRecyclerView();

        // 3. 绑定点击事件
        initListeners();

        checkAndStartGame();



    }

    /**
     * 【核心逻辑】检查是“继续游戏”还是“新游戏”
     */
    private void checkAndStartGame() {
        // 1. 显示加载中
        setLoadingState(true);

        // 2. 使用 DBHelper 加载历史记录
        DBHelper.loadHistory(this, scriptId, new DataCallback<List<ChatMessage>>() {
            @Override
            public void onSuccess(List<ChatMessage> history) {
                setLoadingState(false);

                if (history != null && !history.isEmpty()) {
                    // --- A. 有存档：恢复显示 ---
                    Log.d("ChatActivity", "发现本地存档，条数: " + history.size());

                    messageList.clear();
                    messageList.addAll(history);
                    chatAdapter.notifyDataSetChanged();
                    if (!messageList.isEmpty()) {
                        recyclerView.scrollToPosition(messageList.size() - 1);
                    }

                    // 3. 恢复非消息类的状态 (SessionId, ChapterIndex)
                    // 因为 DBHelper 只存了消息，游戏进度我们需要从 SP 里读出来
                    restoreGameStateFromSP();

                } else {
                    // --- B. 无存档：开启新游戏 ---
                    Log.d("ChatActivity", "无本地存档，开始新游戏");

                    // 确保会话表里有这个 script 的记录 (用于列表页显示)
                    DBHelper.getSessionAndCreateIfNotExist(ChatActivity.this, scriptId, scriptTitle, new DataCallback<ChatSessionEntity>() {
                        @Override
                        public void onSuccess(ChatSessionEntity data) {
                            // 创建完会话记录后，发起网络请求开局
                            startNewGameSession();
                        }

                        @Override
                        public void onFailure(String errorMessage) {

                        }
                    });
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoadingState(false);
                Toast.makeText(ChatActivity.this, "加载存档失败: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    //todo
    private void restoreGameSession() {
        setLoadingState(true);


    }

    private void retrieveIntentData() {

        // 获取所有角色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 (API 33) 及以上：需要传入具体的类类型
            // 注意：这里我们传入 ArrayList.class
            allCharacters = getIntent().getSerializableExtra("ALL_CHARACTERS", ArrayList.class);
        } else {
            // 旧版本写法
            Serializable serializable = getIntent().getSerializableExtra("ALL_CHARACTERS");
            if (serializable instanceof List) {
                // 强转 (Java 会报 unchecked cast 警告，忽略即可)
                allCharacters = (List<CharacterItem>) serializable;
            }
        }
        // 空值安全处理 (防止没传数据导致崩溃)
        if (allCharacters == null) {
            allCharacters = new ArrayList<>(); // 给个空列表防爆
            // 这里可以补救：比如调用网络接口重新拉取
        }

        // 将 List 转为 Map (为了后续快速查找头像)
        initCharacterMap(allCharacters);

        // 获取 ScriptID (假设你也传了这个)
        if (getIntent().hasExtra("SCRIPT_ID")) {
            scriptId = getIntent().getStringExtra("SCRIPT_ID");
        }
        if(getIntent().hasExtra("SCRIPT_TITLE")){
            scriptTitle = getIntent().getStringExtra("SCRIPT_TITLE");
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

            // 创建一个假的 CharacterItem//注意，这里应该给占位
            currentUserRole = new CharacterItem("c_detective","福尔摩斯 (测试)","https://example.com/avatar.png","23","我是来测试的侦探","介绍");

            // 给 scriptId 也赋个默认值
            if (scriptId == null) scriptId = "script_1";

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
        tvTitle.setText(scriptTitle);
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
        TextView tvTitle_bg = view.findViewById(R.id.tvDialogTitle);
        TextView tvContent = view.findViewById(R.id.tvDialogContent);
        ImageView btnClose = view.findViewById(R.id.btnClose);

        tvTitle_bg.setText(currentChapterTitle);
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
        if (id == R.id.restart_game) {
            // 处理搜索逻辑
            // 弹出确认框
            new AlertDialog.Builder(this)
                    .setTitle("重新开始")
                    .setMessage("确定要清空当前进度并重新开始吗？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        performRestart();
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private void performRestart() {
        setLoadingState(true);

        // 1. 清空数据库消息
        DBHelper.deleteChatHistory(this, scriptId, () -> {
            // 2. 清空 SP 里的进度
            getSharedPreferences("GamePrefs", MODE_PRIVATE).edit()
                    .remove("session_" + scriptId)
                    .remove("chapter_" + scriptId)
                    .apply();

            // 3. 清空内存列表
            messageList.clear();
            chatAdapter.notifyDataSetChanged();

            // 4. 重新开局
            currentChapterIndex = 0;
            startNewGameSession();
        });
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
                scriptId,
                currentUserRole.getId(),
                "Qwen/Qwen2.5-72B-Instruct"
        );

        RetrofitClient.getApiService().createSession(requestBody).enqueue(new Callback<SessionResponse>() {

            @Override
            public void onResponse(Call<SessionResponse> call, Response<SessionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Retrofit 已经帮你把 JSON 转成了 SessionResponse 对象
                    SessionResponse data = response.body();
                    sessionId = data.getSessionId();
                    // 【修改点 2】开局成功后，立刻保存 SessionId
                    saveSessionLocally(sessionId);

                    runOnUiThread(() -> {
//                        Toast.makeText(ChatActivity.this, "开局成功", Toast.LENGTH_SHORT).show();
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
                        currentScriptNarration = data.getNarration().replace("【旁白】", "");
                    } else {
                        currentScriptNarration = "当前章节无额外剧本内容。";
                    }
                    currentChapterTitle = "第 " + currentChapterIndex + " 章";

                    // --- B. 更新 Toolbar 标题 ---
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setSubtitle("第 " + currentChapterIndex + " 章");
                    }
                    tvSubtitle.setText("CHAPTER "+currentChapterIndex+"· 第" + currentChapterIndex + " 章");

                    // --- C. 构建并插入 UI 消息 ---



                    // 2. 开放性问题 (系统提示 / System Message)
                    if (data.getDiscussionQuestion() != null && !data.getDiscussionQuestion().isEmpty()) {
                        ChatMessage systemMsg = new ChatMessage(
                                scriptId,
                                sessionId,
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
                    //放在这里对吗？
                    saveGameStateToSP();

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
        addMessageToChat(
                new ChatMessage(
                        scriptId,
                        sessionId,
                        currentUserRole.getName(),
                        currentUserRole.getAvatar(),
                        currentUserRole.getId(),
                        text,
                        ChatMessage.TYPE_USER
                )
        );


        // 2. 发起请求
        MessageRequest request = new MessageRequest(text);
        Call<ResponseBody> call = RetrofitClient.getApiService().sendMessageStream(sessionId, request);

        // 3. 使用新的 Handler
        MultiRoleStreamHandler.handle(call, new MultiRoleStreamHandler.MultiRoleCallback() {

            // 记录当前正在更新的那条消息
            private ChatMessage currentStreamingMsg = null;

            @Override
            public void onSwitchRole(String roleName) {
                // 【核心修复 1】: 在切换新角色之前，把上一条完整的消息存入数据库
                if (currentStreamingMsg != null) {
                    DBHelper.insertMessage(ChatActivity.this, currentStreamingMsg);
                }

                // 【关键】检测到新角色，创建新气泡
                currentStreamingMsg = new ChatMessage(
                        scriptId,
                        sessionId,
                        getIdByName(roleName),
                        roleName,
                        getAvatarByName(roleName),
                        text,
                        ChatMessage.TYPE_PLOT
                );

                // 模拟头像：实际开发中你可以根据 roleName 去查找头像 URL
                // currentStreamingMsg.setAvatarUrl(findAvatarByName(roleName));

                // 3. 更新 UI，原本的
                chatAdapter.addMessage(currentStreamingMsg);
                recyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);

            }

            @Override
            public void onAppendContent(String content) {
                if (currentStreamingMsg == null) {
                    // 如果还没检测到角色名就来了内容（比如旁白，或者第一句话没带名字）
                    // 我们可以创建一个默认的“旁白”或者“系统”消息
                    currentStreamingMsg = new ChatMessage(
                            scriptId,
                            sessionId,
                            "",
                            ChatMessage.TYPE_SYSTEM
                    );


                    //这里是addmessage的代替
                    chatAdapter.addMessage(currentStreamingMsg);
                    recyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);

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
                if (currentStreamingMsg != null) {
                    DBHelper.insertMessage(ChatActivity.this, currentStreamingMsg);
                }
            }

            @Override
            public void onError(Throwable t) {
                setLoadingState(false);
                Toast.makeText(ChatActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onComplete(String fullRawContent) {
                // 【在此处保存数据】
                 Log.d("ChatDebug", "流式接收结束，完整内容为:\n" + fullRawContent);
            }
        });
    }




    /**
     * 4. API: POST /sessions/{id}/vote
     * 投票并结束游戏
     */
    private void showVoteDialog() {
        // 1. 【体验优化】显示一个 Loading，告诉用户正在拉取名单
        // (假设你有一个通用的 loadingDialog，如果没有可以用 Toast 代替)
        ProgressDialog loadingDialog = new ProgressDialog(ChatActivity.this);
        loadingDialog.setMessage("正在获取嫌疑人名单...");
        loadingDialog.setCancelable(false); // 禁止点击外部取消
        loadingDialog.show();

        RetrofitClient.getApiService().getScriptDetail(scriptId).enqueue(new Callback<ScriptDetailModel>() {
            @Override
            public void onResponse(Call<ScriptDetailModel> call, Response<ScriptDetailModel> response) {
                // 请求结束，关闭 Loading
                loadingDialog.dismiss();

                if (response.isSuccessful() && response.body() != null) {
                    ScriptDetailModel scriptDetail = response.body();
                    List<CharacterItem> originalList = scriptDetail.getCharacters();

                    if (originalList == null || originalList.isEmpty()) {
                        Toast.makeText(ChatActivity.this, "未找到角色数据", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 2. 【数据处理】分离显示列表(Names)和数据列表(Real Objects)
                    List<String> displayNames = new ArrayList<>();
                    List<CharacterItem> selectableCharacters = new ArrayList<>();

                    for (CharacterItem character : originalList) {
//                          过滤掉当前玩家自己，防止投自己
                         if (character.getId().equals(currentUserRole.getId())) continue;

                        selectableCharacters.add(character); // 保存对象，用于取ID
                        displayNames.add(character.getName()); // 保存名字，用于显示
                    }

                    final int[] selectedIndex = {-1};

                    new AlertDialog.Builder(ChatActivity.this)
                            .setTitle("指认凶手")
                            .setSingleChoiceItems(displayNames.toArray(new String[0]), -1, (dialog, which) -> {
                                selectedIndex[0] = which;
                            })
                            .setPositiveButton("确定投票", (dialog, which) -> {
                                if (selectedIndex[0] >= 0) {
                                    // 3. 【核心修正】通过索引找到原始对象，提交 ID
                                    CharacterItem target = selectableCharacters.get(selectedIndex[0]);
                                    submitVote(target.getId(),target.getName());

                                    // 可以在这里打印日志验证
                                    // Log.d("Vote", "投给了: " + target.getName() + " ID: " + target.getId());
                                } else {
                                    // 这里有个小坑：AlertDialog 点击按钮后默认会关闭
                                    // 如果没选人，最好重新弹一下 Toast，或者把这个 Listener 提取出来控制 Dialog 关闭时机
                                    Toast.makeText(ChatActivity.this, "请选择一个嫌疑人", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("取消", null)
                            .show();

                } else {
                    Toast.makeText(ChatActivity.this, "加载失败: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ScriptDetailModel> call, Throwable t) {
                loadingDialog.dismiss();
                Toast.makeText(ChatActivity.this, "网络错误，请检查连接", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //注意，这里应该做一个投票榜单，现在暂时没写
    private void submitVote(String targetId, String targetName) {
        // 1. 【关键优化】立刻给用户反馈！
        // 弹窗关闭瞬间，立刻显示 Loading，填补视觉真空期
        ProgressDialog processingDialog = new ProgressDialog(ChatActivity.this);
        processingDialog.setMessage("正在统计票数并生成结局..."); // 提示语暗示这里需要时间
        processingDialog.setCancelable(false);
        processingDialog.show();

        // 2. 提前禁用输入，防止用户在等待期间乱点
        setInputEnabled(false);
        btnNextStage.setVisibility(View.GONE);
        etInput.setHint("正在等待结果...");

        // 3. 创建请求体
        VoteRequest requestBody = new VoteRequest(targetId);

        // 4. 发起请求
        RetrofitClient.getApiService().voteAndReveal(sessionId, requestBody)
                .enqueue(new Callback<VoteResultModel>() {
                    @Override
                    public void onResponse(Call<VoteResultModel> call, Response<VoteResultModel> response) {
                        // 无论成功失败，先关闭 Loading
                        processingDialog.dismiss();

                        if (response.isSuccessful() && response.body() != null) {
                            VoteResultModel result = response.body();

                            ChatMessage endMsg = new ChatMessage(
                                scriptId,
                                sessionId,
                                    "结局揭晓：你指认了 " + targetName + "。\n\n【真相】\n" + result.getTruth(),
                                    ChatMessage.TYPE_SYSTEM
                            );

                            addMessageToChat(endMsg);

                            // 最终状态
                            etInput.setHint("游戏已结束");

                            // 可以在这里触发显示“详细榜单”的逻辑
                            // showResultBoard(result);

                        } else {
                            Toast.makeText(ChatActivity.this, "投票处理失败: " + response.code(), Toast.LENGTH_SHORT).show();
                            // 失败了，是否允许重试？如果允许，需要恢复 UI
                            recoverUIState();
                        }
                    }

                    @Override
                    public void onFailure(Call<VoteResultModel> call, Throwable t) {
                        processingDialog.dismiss();
                        Toast.makeText(ChatActivity.this, "网络连接超时，请重试", Toast.LENGTH_SHORT).show();
                        recoverUIState();
                    }
                });
    }

    // 辅助方法：失败时恢复界面
    private void recoverUIState() {
        setInputEnabled(true);
        btnNextStage.setVisibility(View.VISIBLE);
        etInput.setHint("请输入...");
    }

    private void saveGameStateToSP() {
        getSharedPreferences("GamePrefs", MODE_PRIVATE).edit()
                .putString("session_" + scriptId, sessionId)
                .putInt("chapter_" + scriptId, currentChapterIndex)
                .putString("narration_" + scriptId, currentScriptNarration)
                .apply();
    }
    private void addMessageToChat(ChatMessage msg) {
        // 1. 确保设置了 scriptId (DBHelper 需要这个字段来更新 SessionSummary)
        msg.setScriptId(this.scriptId);

        // 2. 如果是新产生的消息（没有存过库），通常它的 DB ID 是 0
        // 我们直接异步插入
        DBHelper.insertMessage(this, msg);


        // 3. 更新 UI，原本的
        chatAdapter.addMessage(msg);
        recyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
    }

    private void saveSessionLocally(String sId) {
        getSharedPreferences("GamePrefs", MODE_PRIVATE)
                .edit()
                .putString("session_" + scriptId, sId)
                .apply();
    }

    private void clearLocalSession() {
        getSharedPreferences("GamePrefs", MODE_PRIVATE)
                .edit()
                .remove("session_" + scriptId)
                .apply();
    }
    private void restoreGameStateFromSP() {
        SharedPreferences sp = getSharedPreferences("GamePrefs", MODE_PRIVATE);
        this.sessionId = sp.getString("session_" + scriptId, null);
        this.currentChapterIndex = sp.getInt("chapter_" + scriptId, 0);
        this.currentScriptNarration = sp.getString("narration_" + scriptId, "");

        updateUIState(); // 更新一下标题和按钮文字

        // 如果觉得有必要，可以默默调用一次 loadNextChapter 确保同步
        // loadNextChapter();
    }


    private void initCharacterMap(List<CharacterItem> list) {
        characterMap.clear();
        if (list != null) {
            for (CharacterItem item : list) {
                // 以名字为 Key，Item 为 Value
                // 假设 item.getName() 返回 "王医生"
                if (item.getName() != null) {
                    characterMap.put(item.getName(), item);
                }
            }
        }
    }

    // 供流式回调使用的方法
    private String getAvatarByName(String name) {
        CharacterItem item = characterMap.get(name);
        return item != null ? item.getAvatar() : ""; // 返回空字符串或默认头像URL
    }

    private String getIdByName(String name) {
        CharacterItem item = characterMap.get(name);
        return item != null ? item.getId() : "";
    }

    // ==========================================
    //              辅助 UI 方法
    // ==========================================



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