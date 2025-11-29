package com.ai.aiscriptmurde.utils;

// 这是一个泛型接口，<T> 代表它能接住任何类型的数据
public interface DataCallback<T> {

    void onSuccess(T data);

    // 失败时调用 (errorMessage 是错误原因)
    void onFailure(String errorMessage);
}