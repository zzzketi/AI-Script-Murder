package com.ai.aiscriptmurde.ui.scriptlist;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.ai.aiscriptmurde.R;

public class ContainerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container); // 布局里只要有一个 FrameLayout 叫 fragment_container

        // 获取传来的类型
        int type = getIntent().getIntExtra("type", ScriptListFragment.TYPE_DEFAULT);

        if (savedInstanceState == null) {
            // 复用 ScriptListFragment
            Fragment fragment = ScriptListFragment.newInstance(type);
            
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        }
    }
}