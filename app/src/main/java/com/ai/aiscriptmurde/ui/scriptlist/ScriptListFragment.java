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
        com.ai.aiscriptmurde.network.RetrofitClient.getApiService().getScripts(null)
                .enqueue(new retrofit2.Callback<com.ai.aiscriptmurde.model.ScriptListResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.ai.aiscriptmurde.model.ScriptListResponse> call, retrofit2.Response<com.ai.aiscriptmurde.model.ScriptListResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<ScriptModel> dataList = response.body().getScripts();

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
                            String errorMsg = response.message();

                            // 打印日志到 Logcat
                            android.util.Log.e("ScriptListFragment", "请求失败: code=" + errorCode + ", msg=" + errorMsg);

                            // 提示用户
                            if (getContext() != null) {
                                android.widget.Toast.makeText(getContext(), "获取数据失败 (Code: " + errorCode + ")", android.widget.Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.ai.aiscriptmurde.model.ScriptListResponse> call, Throwable t) {
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
