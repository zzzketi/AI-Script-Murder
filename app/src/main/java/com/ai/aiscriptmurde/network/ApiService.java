package com.ai.aiscriptmurde.network;

import com.ai.aiscriptmurde.model.ScriptDetailModel;
import com.ai.aiscriptmurde.model.ScriptListResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    // 获取列表
    @GET("scripts")
    Call<ScriptListResponse> getScripts(@Query("keyword") String keyword);

    // 获取详情
    @GET("scripts/{id}")
    Call<ScriptDetailModel> getScriptDetail(@Path("id") String id);
}