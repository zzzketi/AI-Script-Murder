package com.ai.aiscriptmurde.ui.main;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

// 引入创建的那三个 Fragment
import com.ai.aiscriptmurde.ui.scriptlist.ScriptListFragment;
import com.ai.aiscriptmurde.ui.discover.DiscoverFragment;
import com.ai.aiscriptmurde.ui.mine.MineFragment;

public class MainPagerAdapter extends FragmentStateAdapter {

    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    /**
     * 核心方法：根据位置返回对应的 Fragment
     * position: 0 -> 第一个 Tab
     * position: 1 -> 第二个 Tab
     * ...
     */
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new ScriptListFragment(); // 对应“剧本”
            case 1:
                return new DiscoverFragment();   // 对应“发现”
            default:
                return new MineFragment();       // 对应“我的”
        }
    }

    /**
     * 告诉 ViewPager 一共有几个 Tab
     */
    @Override
    public int getItemCount() {
        return 3;
    }
}