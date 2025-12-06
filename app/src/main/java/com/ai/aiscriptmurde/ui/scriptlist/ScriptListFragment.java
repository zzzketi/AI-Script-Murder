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

import retrofit2.Call;

public class ScriptListFragment extends Fragment {

    private ScriptAdapter adapter;
    private RecyclerView rv;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_script_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. 找到 RecyclerView
        rv = view.findViewById(R.id.rv_script_list);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        // 2. 读取数据并设置适配器
        fetchScriptsFromNetwork();
    }

    // 读取JSON 文件
    private void fetchScriptsFromNetwork() {
        // 使用 Retrofit 发起请求
        // 1. 泛型改为 List<ScriptModel>
        com.ai.aiscriptmurde.network.RetrofitClient.getApiService().getScripts(null) // 注意这里去掉了 null，或者根据你的定义传参
                .enqueue(new retrofit2.Callback<List<ScriptModel>>() {
                    @Override
                    public void onResponse(retrofit2.Call<List<ScriptModel>> call, retrofit2.Response<List<ScriptModel>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            // 2. 直接获取 List，不需要 .getScripts()
                            List<ScriptModel> dataList = response.body();

                            // 建议做一个非空判断，防止 dataList 为空时 Adapter 报错
                            if (dataList == null) {
                                dataList = new java.util.ArrayList<>();
                            }

                            // 设置适配器
                            adapter = new ScriptAdapter(dataList);
                            adapter.setOnItemClickListener(script -> {
                                Intent intent = new Intent(requireContext(), ScriptDetailActivity.class);
                                intent.putExtra("key_script_id", script.getId());
                                startActivity(intent);
                            });
                            rv.setAdapter(adapter);
                        } else {
                            // === 处理服务器返回错误
                            int errorCode = response.code();
                            android.util.Log.e("ScriptListFragment", "请求失败: code=" + errorCode);
                            if (getContext() != null) {
                                android.widget.Toast.makeText(getContext(), "获取数据失败: " + errorCode, android.widget.Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ScriptModel>> call, Throwable t) {
                        //处理网络层面的失败
                        String errorInfo = t.getMessage();
                        android.util.Log.e("ScriptListFragment", "网络异常: " + errorInfo);

                        if (getContext() != null) {
                            // 提示信息
                            String toastMsg = "网络请求失败，请检查网络";
                            if (errorInfo != null && errorInfo.contains("Failed to connect")) {
                                toastMsg = "无法连接服务器，请检查 Python 后端是否启动";
                            }
                            android.widget.Toast.makeText(getContext(), toastMsg, android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }



                });
    }
}
