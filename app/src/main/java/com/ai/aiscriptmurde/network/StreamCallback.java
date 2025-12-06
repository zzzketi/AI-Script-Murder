package com.ai.aiscriptmurde.network;


public interface StreamCallback {
    // 收到一小段文本时触发 (运行在主线程)
    void onStreamNext(String textChunk);

    // 整个流结束时触发 (运行在主线程)
    void onStreamComplete();

    // 出错时触发 (运行在主线程)
    void onError(Throwable t);
}
