package com.ai.aiscriptmurde.network;

import com.ai.aiscriptmurde.model.CreateSessionRequest;
import com.ai.aiscriptmurde.model.MessageRequest;
import com.ai.aiscriptmurde.model.NextChapterResponse;
import com.ai.aiscriptmurde.model.ScriptDetailModel;
import com.ai.aiscriptmurde.model.ScriptListResponse;
import com.ai.aiscriptmurde.model.ScriptModel;
import com.ai.aiscriptmurde.model.SessionResponse;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;

public interface ApiService {
    // 获取列表
    @GET("scripts")
    Call<List<ScriptModel>> getScripts(@Query("keyword") String keyword);

    // 获取详情
    @GET("scripts/{id}")
    Call<ScriptDetailModel> getScriptDetail(@Path("id") String id);

    // 对应接口: POST /sessions
    @POST("sessions")
    Call<SessionResponse> createSession(@Body CreateSessionRequest request);

    /**
     * 流式发送消息接口
     * * @Streaming: 告诉 Retrofit 不要把数据一次性读进内存，而是实时返回 IO 流。
     * Call<ResponseBody>: 返回原始响应体，我们需要手动读取 InputStream。
     */
    @Streaming
    @POST("sessions/{session_id}/message/stream")
    Call<ResponseBody> sendMessageStream(
            @Path("session_id") String sessionId,
            @Body MessageRequest request
    );

    /**
     * 触发下一章
     * 不需要 @Body，因为参数都在 URL 路径里
     */
    @POST("sessions/{session_id}/next_chapter")
    Call<NextChapterResponse> triggerNextChapter(@Path("session_id") String sessionId);
}