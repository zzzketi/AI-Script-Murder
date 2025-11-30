package com.ai.aiscriptmurde.ui.scriptlist;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity; // 注意继承的是 AppCompatActivity

import com.ai.aiscriptmurde.R;
import com.ai.aiscriptmurde.model.CharacterItem;
import com.ai.aiscriptmurde.model.ScriptDetailModel;
import com.ai.aiscriptmurde.utils.ScriptUtils;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

// 1. 修改继承：extends Fragment -> extends AppCompatActivity
public class ScriptDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.fragment_script_detail);


        ImageView ivBack = findViewById(R.id.iv_back);
        ivBack.setOnClickListener(v -> {
            finish();
        });

        String scriptId = getIntent().getStringExtra("key_script_id");
        
        if (scriptId != null) {
            loadDetailData(scriptId);
        } else {
            Toast.makeText(this, "未获取到剧本ID", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadDetailData(String scriptId) {
        String fileName = "mock_data/details/script_" + scriptId + ".json";
        String jsonStr = ScriptUtils.readAssetFile(this, fileName);

        if (jsonStr != null) {
            Gson gson = new Gson();
            ScriptDetailModel detail = gson.fromJson(jsonStr, ScriptDetailModel.class);
            updateUI(detail);
        } else {
            // 6. Context 修改：getContext() -> this
            Toast.makeText(this, "加载剧本数据失败", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateUI(ScriptDetailModel detail) {
        if (detail == null) return;


        TextView tvTitle = findViewById(R.id.tv_detail_title);
        TextView tvSubtitle = findViewById(R.id.tv_detail_subtitle);
        ImageView ivCover = findViewById(R.id.iv_detail_cover);

        tvTitle.setText(detail.getTitle());
        tvSubtitle.setText(detail.getDesc());

        int coverResId = ScriptUtils.getResId(this,detail.getImage());
        if (coverResId != 0) {
            ivCover.setImageResource(coverResId);
        }else {
            ivCover.setImageResource(R.drawable.ic_launcher_background);
        }

        if (detail.getBackground() != null) {
            TextView tvTime = findViewById(R.id.tv_detail_time);
            TextView tvLocation = findViewById(R.id.tv_detail_location);
            TextView tvContent = findViewById(R.id.tv_detail_content);

            tvTime.setText("时间：" + detail.getBackground().getTime());
            tvLocation.setText("地点：" + detail.getBackground().getLocation());

            List<String> rules = detail.getBackground().getRules();
            if (rules != null && !rules.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < rules.size(); i++) {
                    sb.append("• ").append(rules.get(i));
                    if (i < rules.size() - 1) {
                        sb.append("\n");
                    }
                }
                tvContent.setText(sb.toString());
            } else {
                tvContent.setText("暂无详细规则。");
            }
        }

        LinearLayout charContainer = findViewById(R.id.layout_characters_container);
        addCharacterViews(charContainer, detail.getCharacters(),detail);//新增detail
    }

    private void addCharacterViews(LinearLayout container, List<CharacterItem> list,ScriptDetailModel detail) {
        container.removeAllViews();

        if (list == null || list.isEmpty()) return;

        LayoutInflater inflater = LayoutInflater.from(this);

        for (CharacterItem item : list) {
            View itemView = inflater.inflate(R.layout.item_character, container, false);

            TextView tvName = itemView.findViewById(R.id.tv_char_name);
            TextView tvDesc = itemView.findViewById(R.id.tv_char_desc);
            ImageView ivAvatar = itemView.findViewById(R.id.iv_char_avatar);

            tvName.setText(item.getName());
            tvDesc.setText(item.getDesc());


            int avatarId = ScriptUtils.getResId(this,item.getAvatar());
            if (avatarId != 0) {
                ivAvatar.setImageResource(avatarId);
            }
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 1. 创建意图：从当前页面 -> ChatActivity
                    Intent intent = new Intent(ScriptDetailActivity.this, CharacterInfoActivity.class);


                    // 1. 传递角色对象
                    intent.putExtra("key_selected_character", item);

                    // 🔥🔥🔥 [新增] 传递核心数据给 CharacterInfo -> ChatActivity
                    intent.putExtra("SCRIPT_ID", detail.getId());
                    intent.putExtra("SCRIPT_TITLE", detail.getTitle());

                    // 🔥🔥🔥 [新增] 传递 AI 设定 (System Prompt)
                    if (detail.getSystemPrompt() != null) {
                        intent.putExtra("SYSTEM_PROMPT", detail.getSystemPrompt());
                    } else {
                        intent.putExtra("SYSTEM_PROMPT", "你是剧本杀主持人。"); // 默认值防崩
                    }

                    // 🔥🔥🔥 [新增] 传递背景故事 (用于聊天页顶部便签)
                    if (detail.getBackground() != null) {
                        // 优先传 Story，如果没有就传 Rules 拼成的字符串
                        intent.putExtra("BACKGROUND", detail.getBackground().getStory());
                    }

                    // 3. 开始跳转
                    startActivity(intent);
                }
            });

            container.addView(itemView);
        }
    }

}