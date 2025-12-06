package com.ai.aiscriptmurde.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    // 模拟器访问本机电脑 localhost 专用 IP
    private static final String BASE_URL = "http://localhost:8000/";

    private static Retrofit retrofit = null;
    private static RetrofitClient instance;

    public static ApiService getApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }

}