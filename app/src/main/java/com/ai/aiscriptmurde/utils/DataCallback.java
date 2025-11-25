package com.ai.aiscriptmurde.utils;

// 这是一个泛型接口，<T> 代表它能接住任何类型的数据
public interface DataCallback<T> {
    void onResult(T data);
}