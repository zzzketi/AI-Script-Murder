package com.ai.aiscriptmurde.network;

import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StreamManager {

    // 用于把后台线程的数据发送回主线程 (UI线程)
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void handleStream(Call<ResponseBody> call, StreamCallback callback) {
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 开启子线程去读取流，不能在主线程做 IO 操作
                    new Thread(() -> {
                        readStream(response.body(), callback);
                    }).start();
                } else {
                    postError(callback, new Exception("Server Error: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                postError(callback, t);
            }
        });
    }

    private static void readStream(ResponseBody body, StreamCallback callback) {
        InputStream inputStream = null;
        BufferedReader reader = null;

        try {
            inputStream = body.byteStream();
            reader = new BufferedReader(new InputStreamReader(inputStream));

            // 这里假设服务器返回的是纯文本流。
            // 如果是 SSE (data: ...)，需要做字符串切割处理。
            int charCode;
            char[] buffer = new char[1024]; // 缓冲区
            int len;

            // 循环读取数据流
            while ((len = reader.read(buffer)) != -1) {
                String chunk = new String(buffer, 0, len);

                // 切换到主线程更新 UI
                mainHandler.post(() -> callback.onStreamNext(chunk));
            }

            // 读取完毕
            mainHandler.post(callback::onStreamComplete);

        } catch (IOException e) {
            postError(callback, e);
        } finally {
            // 关闭流
            try {
                if (reader != null) reader.close();
                if (inputStream != null) inputStream.close();
            } catch (IOException ignored) {}
        }
    }

    private static void postError(StreamCallback callback, Throwable t) {
        mainHandler.post(() -> callback.onError(t));
    }
}