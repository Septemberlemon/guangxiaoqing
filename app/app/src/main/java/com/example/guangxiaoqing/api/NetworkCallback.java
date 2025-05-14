package com.example.guangxiaoqing.api;

/**
 * 网络请求回调接口
 * @param <T> 响应数据类型
 */
public interface NetworkCallback<T> {
    void onSuccess(T response);
    void onError(String errorMessage);
}