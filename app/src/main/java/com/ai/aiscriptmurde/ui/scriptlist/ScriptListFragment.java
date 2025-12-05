package com.ai.aiscriptmurde.ui.scriptlist;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.ai.aiscriptmurde.R;
import android.content.Intent;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ai.aiscriptmurde.model.ScriptModel;

import java.util.List;

public class ScriptListFragment extends Fragment {
    public static final int TYPE_DEFAULT = 0; // 默认推荐
    public static final int TYPE_SCORE = 1;  // 高分榜

    private int currentType = TYPE_DEFAULT;

    private ScriptAdapter adapter;
    private RecyclerView rv;
    private TextView tvTitle;
    private View btnLatest; // 右上角按钮
    private ImageView ivBack;
    private View searchContainer;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_script_list, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentType = getArguments().getInt("key_type", TYPE_DEFAULT);
        }
    }

    public static ScriptListFragment newInstance(int type) {
        ScriptListFragment fragment = new ScriptListFragment();
        Bundle args = new Bundle();
        args.putInt("key_type", type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rv = view.findViewById(R.id.rv_script_list);
        tvTitle = view.findViewById(R.id.tv_page_title);
        btnLatest = view.findViewById(R.id.btn_latest);
        ivBack = view.findViewById(R.id.iv_back);
        searchContainer = view.findViewById(R.id.ll_search_container);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        // 根据类型调整 UI
        setupUIByType();

        //  加载数据
        fetchScriptsFromNetwork();
    }

    private void setupUIByType() {
        if (currentType == TYPE_DEFAULT) {
            tvTitle.setText("剧本大厅");
            btnLatest.setVisibility(View.VISIBLE);
            ivBack.setVisibility(View.GONE);
            // 点击“最新”，跳转到一个新的 Activity
            // 这里复用 Fragment，传入 TYPE_LATEST
            btnLatest.setOnClickListener(v -> {
                // 启动承载 Fragment 的容器 Activity
                Intent intent = new Intent(getContext(), ContainerActivity.class);
                intent.putExtra("type", TYPE_SCORE);
                startActivity(intent);
            });

        } else if (currentType == TYPE_SCORE) {
            tvTitle.setText("高分榜");
            btnLatest.setVisibility(View.GONE); // 隐藏按钮
            searchContainer.setVisibility(View.GONE); // 隐藏搜索框
            //设置返回按钮
            ivBack.setVisibility(View.VISIBLE);
            ivBack.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().finish();
                }
            });
        }
    }

    // 读取JSON 文件
    private void fetchScriptsFromNetwork() {

        retrofit2.Call<com.ai.aiscriptmurde.model.ScriptListResponse> call;


        if (currentType == TYPE_SCORE) {
            call = com.ai.aiscriptmurde.network.RetrofitClient.getApiService().getScriptsAsScore();
        } else {
            call = com.ai.aiscriptmurde.network.RetrofitClient.getApiService().getScripts(null);
        }

        // 统一发起请求
        call.enqueue(new retrofit2.Callback<com.ai.aiscriptmurde.model.ScriptListResponse>() {
            @Override
            public void onResponse(retrofit2.Call<com.ai.aiscriptmurde.model.ScriptListResponse> call, retrofit2.Response<com.ai.aiscriptmurde.model.ScriptListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ScriptModel> dataList = response.body().getScripts();

                    boolean shouldShowScore = (currentType == TYPE_SCORE);

                    adapter = new ScriptAdapter(dataList, shouldShowScore);
                    // 设置适配器
                    adapter = new ScriptAdapter(dataList,shouldShowScore);
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

                    android.util.Log.e("ScriptListFragment", "请求失败: code=" + errorCode + ", msg=" + errorMsg);

                    if (getContext() != null) {
                        android.widget.Toast.makeText(getContext(), "获取数据失败 (Code: " + errorCode + ")", android.widget.Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.ai.aiscriptmurde.model.ScriptListResponse> call, Throwable t) {
                // 处理网络层面的失败
                String errorInfo = t.getMessage();
                android.util.Log.e("ScriptListFragment", "网络异常: " + errorInfo);

                if (getContext() != null) {
                    String toastMsg = "网络请求失败，请检查网络";
                    if (errorInfo != null && errorInfo.contains("Failed to connect")) {
                        toastMsg = "无法连接服务器，请检查后端是否启动";
                    }
                    android.widget.Toast.makeText(getContext(), toastMsg, android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
