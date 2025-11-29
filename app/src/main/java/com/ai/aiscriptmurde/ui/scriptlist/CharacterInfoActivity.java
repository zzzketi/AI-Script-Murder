package com.ai.aiscriptmurde.ui.scriptlist;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ai.aiscriptmurde.R;
import com.ai.aiscriptmurde.model.CharacterItem;
import com.ai.aiscriptmurde.utils.ScriptUtils;

public class CharacterInfoActivity extends AppCompatActivity {

    private CharacterItem selectedCharacter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.character_info);

        // 从Intent获取选中的角色信息
        selectedCharacter = (CharacterItem) getIntent().getSerializableExtra("key_selected_character");

        if (selectedCharacter == null) {
            Toast.makeText(this, "未获取到角色信息", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 初始化视图组件
        initViews();

        // 显示角色信息
        displayCharacterInfo();
    }

    private void initViews() {
        ImageView ivBack = findViewById(R.id.iv_back);
        ImageView ivRefresh = findViewById(R.id.iv_refresh);
        Button btnSelect = findViewById(R.id.btn_select);

        // 设置返回按钮点击事件
        ivBack.setOnClickListener(v -> finish());

        // 设置刷新按钮点击事件（如果需要刷新功能）
        ivRefresh.setOnClickListener(v -> {
            // 刷新角色信息的逻辑（如果需要）
            displayCharacterInfo();
            Toast.makeText(this, "信息已刷新", Toast.LENGTH_SHORT).show();
        });

        // 设置选择角色按钮点击事件
        btnSelect.setOnClickListener(v -> {
            // 处理选择角色的逻辑
            selectCharacter();
        });
    }

    private void displayCharacterInfo() {
        if (selectedCharacter == null) return;

        TextView tvName = findViewById(R.id.tv_character_name);
        TextView tvAge = findViewById(R.id.tv_character_age);
        TextView tvIntroduction = findViewById(R.id.tv_character_introduction);
        ImageView ivAvatar = findViewById(R.id.iv_character_avatar);

        // 设置角色基本信息
        tvName.setText(selectedCharacter.getName());

        // 设置年龄信息，如果有
        if (selectedCharacter.getAge() != null && !selectedCharacter.getAge().isEmpty()) {
            tvAge.setText("年龄：" + selectedCharacter.getAge());
            tvAge.setVisibility(View.VISIBLE);
        } else {
            tvAge.setVisibility(View.GONE);
        }

        // 设置角色详细介绍
        if (selectedCharacter.getIntroduction() != null && !selectedCharacter.getIntroduction().isEmpty()) {
            tvIntroduction.setText(selectedCharacter.getIntroduction());
        } else if (selectedCharacter.getDesc() != null && !selectedCharacter.getDesc().isEmpty()) {
            // 如果没有详细介绍，则显示简短描述
            tvIntroduction.setText(selectedCharacter.getDesc());
        } else {
            tvIntroduction.setText("暂无角色介绍信息");
        }

        // 设置角色头像
        if (selectedCharacter.getAvatar() != null && !selectedCharacter.getAvatar().isEmpty()) {
            int avatarId = ScriptUtils.getResId(this, selectedCharacter.getAvatar());
            if (avatarId != 0) {
                ivAvatar.setImageResource(avatarId);
            } else {
                // 如果找不到对应的头像资源，使用默认头像
                ivAvatar.setImageResource(R.drawable.ic_launcher_background);
                Log.w("CharacterInfoActivity", "未找到角色头像资源: " + selectedCharacter.getAvatar());
            }
        } else {
            // 如果没有头像信息，使用默认头像
            ivAvatar.setImageResource(R.drawable.ic_launcher_background);
        }
    }

    private void selectCharacter() {
        // 这里可以实现选择角色后的逻辑，例如保存选择状态、跳转到游戏界面等
        Toast.makeText(this, "已选择角色：" + selectedCharacter.getName(), Toast.LENGTH_SHORT).show();

        // 如果需要，可以将选择结果返回给上一个界面
        // Intent resultIntent = new Intent();
        // resultIntent.putExtra("selected_character", selectedCharacter);
        // setResult(RESULT_OK, resultIntent);

        // 完成后返回
        finish();
    }
}