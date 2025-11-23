package com.ai.aiscriptmurde.ui.scriptlist;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;


import com.ai.aiscriptmurde.R;

public class CharacterInfoActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.character_info);

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
            // 处理选择角色逻辑...待续
        });
    }
}
