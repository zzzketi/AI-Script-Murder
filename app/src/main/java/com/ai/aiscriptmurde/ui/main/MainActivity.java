package com.ai.aiscriptmurde.ui.main;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;

import com.ai.aiscriptmurde.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //获取id
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        // 1. 设置 Adapter
        viewPager.setAdapter(new MainPagerAdapter(this));

        // 2. 核心：处理 ViewPager 滑动 -> 联动底部按钮
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                bottomNav.getMenu().getItem(position).setChecked(true);
            }
        });

        // 3. 核心：处理 底部按钮点击 -> 联动 ViewPager 滑动
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_script) { // 你的menu id
                viewPager.setCurrentItem(0);
                return true;
            } else if (itemId == R.id.nav_discover) {
                viewPager.setCurrentItem(1);
                return true;
            } else if (itemId == R.id.nav_mine) {
                viewPager.setCurrentItem(2);
                return true;
            }
            return false;
        });
    }
}