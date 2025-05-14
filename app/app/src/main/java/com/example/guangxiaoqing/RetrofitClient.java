package com.example.guangxiaoqing;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "http://47.97.48.127/";  // 通过反向代理访问API
    // 确保日志输出到控制台，方便调试
    private static RetrofitClient instance;
    private Retrofit retrofit;

    // 添加TAG常量用于日志输出
    private static final String TAG = "RetrofitClient";

    private RetrofitClient() {
        // 创建 OkHttpClient，添加日志拦截器
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> {
            android.util.Log.d(TAG, message);
        });
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        android.util.Log.d(TAG, "初始化Retrofit，BASE_URL: " + BASE_URL);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                // 添加HTTP/1.1协议强制使用拦截器
                .addInterceptor(chain -> {
                    okhttp3.Request original = chain.request();
                    okhttp3.Request request = original.newBuilder()
                            .header("Connection", "keep-alive")
                            .header("Protocol", "HTTP/1.1")
                            .method(original.method(), original.body())
                            .build();
                    return chain.proceed(request);
                })
                // 显式设置HTTP协议版本
                .protocols(java.util.Arrays.asList(okhttp3.Protocol.HTTP_1_1))
                .build();

        // 创建 Retrofit 实例
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }

    public ApiService getApiService() {
        return retrofit.create(ApiService.class);
    }
}
