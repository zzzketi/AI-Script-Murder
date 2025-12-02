package com.ai.aiscriptmurde.ui.scriptlist;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;


import com.ai.aiscriptmurde.R;

import com.ai.aiscriptmurde.model.CharacterItem;
import com.ai.aiscriptmurde.ui.chat.ChatActivity;


public class CharacterInfoActivity extends AppCompatActivity {
    // 用来暂存剧本的核心数据
    private String scriptId;
    private String systemPrompt;
    private String scriptTitle;
    private String backgroundStory;
    private CharacterItem currentRole; // 当前选中的角色
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.character_info);

        // 1. 接收上一个页面（列表页）传来的所有数据
        // 注意：这要求你在列表页跳转到这里时，必须把这些 putExtra 进来！
        scriptId = getIntent().getStringExtra("SCRIPT_ID");
        systemPrompt = getIntent().getStringExtra("SYSTEM_PROMPT");
        scriptTitle = getIntent().getStringExtra("SCRIPT_TITLE");
        backgroundStory = getIntent().getStringExtra("BACKGROUND");

        // 接收角色对象
        currentRole = (CharacterItem) getIntent().getSerializableExtra("key_selected_character");


        // 初始化视图组件
        initViews();

    }

    private void initViews() {
        ImageView ivBack = findViewById(R.id.iv_back);
        ImageView ivRefresh = findViewById(R.id.iv_refresh);
        Button btnSelect = findViewById(R.id.btn_select);

        // 设置点击事件
        ivBack.setOnClickListener(v -> finish());
        btnSelect.setOnClickListener(v -> {
            Intent intent = new Intent(CharacterInfoActivity.this, ChatActivity.class);

            // 🔥 核心：把接力棒（数据）传给 ChatActivity
            intent.putExtra("SCRIPT_ID", scriptId);
            intent.putExtra("SYSTEM_PROMPT", systemPrompt);
            intent.putExtra("SCRIPT_TITLE", scriptTitle);
            intent.putExtra("BACKGROUND", backgroundStory);

            // 还可以把用户选的角色传过去（ChatActivity暂时还没处理这个，但建议先传）
            if (currentRole != null) {
                intent.putExtra("USER_ROLE", currentRole);
            }

            startActivity(intent);

            // 选完角色就不能退回这里了，把当前页面关掉
             finish();
        });
    }
}
