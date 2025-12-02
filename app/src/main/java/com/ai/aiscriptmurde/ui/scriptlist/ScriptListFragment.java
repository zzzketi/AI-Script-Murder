package com.ai.aiscriptmurde.ui.scriptlist;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.ai.aiscriptmurde.R;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ai.aiscriptmurde.R;
import com.ai.aiscriptmurde.model.ScriptModel;
import com.ai.aiscriptmurde.utils.ScriptUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;

public class ScriptListFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_script_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. 找到 RecyclerView
        RecyclerView rv = view.findViewById(R.id.rv_script_list);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        // 2. 读取数据
        List<ScriptModel> dataList = getScriptsFromJson();

        // 3. 设置适配器
        ScriptAdapter adapter = new ScriptAdapter(dataList);
        adapter.setOnItemClickListener(new ScriptAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ScriptModel script) {
                Intent intent = new Intent(requireContext(), ScriptDetailActivity.class);
                intent.putExtra("key_script_id", script.getId());
                startActivity(intent);
            }
        });
        rv.setAdapter(adapter);
    }

    // 读取JSON 文件
    private List<ScriptModel> getScriptsFromJson() {
        String jsonStr = ScriptUtils.readAssetFile(requireContext(),"mock_data/script_list.json");

        // 使用 Gson 解析 JSON 数组
        Gson gson = new Gson();
        Type listType = new TypeToken<List<ScriptModel>>(){}.getType();
        return gson.fromJson(jsonStr, listType);
    }
}
