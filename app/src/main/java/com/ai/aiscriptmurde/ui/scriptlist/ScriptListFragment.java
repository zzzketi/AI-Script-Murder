package com.ai.aiscriptmurde.ui.scriptlist;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

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
import java.util.ArrayList;
import java.util.List;

public class ScriptListFragment extends Fragment {

    private RecyclerView rvScriptList;
    private ScriptAdapter adapter;
    private List<ScriptModel> allScripts; // 存储所有剧本数据
    private List<ScriptModel> filteredScripts; // 存储过滤后的剧本数据

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_script_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. 找到 RecyclerView
        rvScriptList = view.findViewById(R.id.rv_script_list);
        rvScriptList.setLayoutManager(new LinearLayoutManager(getContext()));

        // 2. 读取数据
        allScripts = getScriptsFromJson();
        filteredScripts = new ArrayList<>(allScripts); // 初始时显示所有剧本

        // 3. 设置适配器
        adapter = new ScriptAdapter(filteredScripts);
        adapter.setOnItemClickListener(new ScriptAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ScriptModel script) {
                Intent intent = new Intent(requireContext(), ScriptDetailActivity.class);
                intent.putExtra("key_script_id", script.getId());
                startActivity(intent);
            }
        });
        rvScriptList.setAdapter(adapter);

        // 4. 设置搜索框监听
        EditText etSearch = view.findViewById(R.id.et_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterScripts(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 5. 设置搜索按钮监听（键盘搜索）
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                filterScripts(v.getText().toString());
                // 隐藏键盘
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return true;
            }
            return false;
        });
    }

    // 搜索过滤方法
    private void filterScripts(String query) {
        filteredScripts.clear();

        if (query.isEmpty()) {
            // 如果搜索框为空，显示所有剧本
            filteredScripts.addAll(allScripts);
        } else {
            // 根据标题进行过滤（忽略大小写）
            String lowerCaseQuery = query.toLowerCase();
            for (ScriptModel script : allScripts) {
                if (script.getTitle().toLowerCase().contains(lowerCaseQuery)) {
                    filteredScripts.add(script);
                }
            }
        }

        // 通知适配器数据发生变化
        adapter.notifyDataSetChanged();
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