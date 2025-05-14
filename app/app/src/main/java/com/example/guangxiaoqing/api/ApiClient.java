package com.example.guangxiaoqing.api;

import android.content.Context;
import android.util.Log;

import com.example.guangxiaoqing.UserSession;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * API客户端，用于创建Retrofit实例和提供API服务
 */
public class ApiClient {
    private static final String TAG = "ApiClient";
    // 后端服务器地址
    public static final String BASE_URL = "http://47.97.48.127/api/";  // 通过反向代理访问API
    private static Retrofit retrofit = null;
    private static ApiService apiService = null;

    /**
     * 获取Retrofit实例
     * @param context 上下文
     * @return Retrofit实例
     */
    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            // 直接使用createOkHttpClient方法创建的客户端
            OkHttpClient client = createOkHttpClient(context);

            // 创建Retrofit实例
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    /**
     * 获取API服务接口
     * @param context 上下文
     * @return API服务接口
     */
    public static ApiService getApiService(Context context) {
        if (apiService == null) {
            apiService = getClient(context).create(ApiService.class);
        }
        return apiService;
    }

    /**
     * 获取带认证的API服务
     * @param context 上下文
     * @return 带认证头的API服务
     */
    public static String getAuthHeader(Context context) {
        UserSession userSession = UserSession.getInstance(context);
        String token = userSession.getToken();
        if (token != null && !token.isEmpty()) {
            return "Bearer " + token;
        }
        return null;
    }

    // 创建OkHttpClient实例时增加超时时间和重试机制
    private static OkHttpClient createOkHttpClient(Context context) {
        // 创建日志拦截器
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // 使用 OkHttpClient.Builder 而不是直接使用 OkHttpClient
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)  // 连接超时时间
            .readTimeout(120, TimeUnit.SECONDS)    // 增加读取超时时间到120秒，以处理长回答
            .writeTimeout(15, TimeUnit.SECONDS)    // 写入超时时间
            .retryOnConnectionFailure(true);       // 启用连接失败重试

        // 添加日志拦截器
        builder.addInterceptor(loggingInterceptor);

        // 添加重试拦截器
        builder.addInterceptor(chain -> {
            okhttp3.Request request = chain.request();

            // 生成请求ID用于日志跟踪
            String requestId = String.format("%08x", System.currentTimeMillis() & 0xFFFFFFFF);
            Log.d(TAG, "[" + requestId + "] 发送请求: " + request.url());

            // 最大重试次数
            int maxRetries = 3;
            int retryCount = 0;

            okhttp3.Response response = null;
            while (retryCount < maxRetries) {
                try {
                    // 如果不是第一次尝试，添加重试信息到请求头
                    if (retryCount > 0) {
                        request = request.newBuilder()
                                .header("X-Retry-Count", String.valueOf(retryCount))
                                .build();
                        Log.d(TAG, "[" + requestId + "] 第" + retryCount + "次重试请求: " + request.url());
                    }

                    // 执行请求
                    response = chain.proceed(request);

                    // 如果响应成功或者是流式响应，直接返回
                    if (response.isSuccessful() || request.url().toString().contains("/chat/stream")) {
                        Log.d(TAG, "[" + requestId + "] 请求成功: " + response.code());
                        return response;
                    }

                    // 如果是服务器错误(5xx)，关闭响应并重试
                    if (response.code() >= 500) {
                        Log.d(TAG, "[" + requestId + "] 服务器错误: " + response.code() + "，准备重试");
                        response.close();
                        retryCount++;
                        // 指数退避策略
                        Thread.sleep(1000 * (1 << retryCount));
                        continue;
                    }

                    // 其他错误码直接返回
                    return response;

                } catch (Exception e) {
                    Log.e(TAG, "[" + requestId + "] 请求异常: " + e.getMessage(), e);

                    // 关闭之前的响应（如果有）
                    if (response != null) {
                        response.close();
                    }

                    // 增加重试计数
                    retryCount++;

                    // 如果已达到最大重试次数，抛出异常
                    if (retryCount >= maxRetries) {
                        throw new RuntimeException("请求失败，已重试" + retryCount + "次: " + e.getMessage(), e);
                    }

                    // 等待一段时间后重试
                    try {
                        // 指数退避策略
                        Thread.sleep(1000 * (1 << retryCount));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重试等待被中断", ie);
                    }
                }
            }

            // 这里不应该到达，但为了编译通过
            throw new RuntimeException("请求失败，已重试" + retryCount + "次");
        });

        // 添加HTTP/1.1协议强制使用拦截器
        builder.addInterceptor(chain -> {
            okhttp3.Request original = chain.request();
            okhttp3.Request request = original.newBuilder()
                    .header("Connection", "keep-alive")
                    .header("Protocol", "HTTP/1.1")
                    .method(original.method(), original.body())
                    .build();
            return chain.proceed(request);
        });

        // 添加流式响应处理拦截器
        builder.addInterceptor(chain -> {
            okhttp3.Request request = chain.request();
            // 检查是否是流式API请求
            if (request.url().toString().contains("/chat/stream")) {
                String requestId = String.format("%08x", System.currentTimeMillis() & 0xFFFFFFFF);
                Log.d(TAG, "[" + requestId + "] 检测到流式API请求: " + request.url());

                // 确保不会过早关闭连接，添加更多的请求头以支持流式响应
                request = request.newBuilder()
                        .header("Accept", "text/plain")
                        .header("Cache-Control", "no-cache")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .header("Connection", "keep-alive")
                        .header("X-Request-ID", requestId)
                        .build();
                Log.d(TAG, "[" + requestId + "] 已添加流式响应所需的请求头");
            }
            okhttp3.Response response = chain.proceed(request);
            return response;
        });

        // 显式设置HTTP协议版本
        builder.protocols(java.util.Arrays.asList(okhttp3.Protocol.HTTP_1_1));

        return builder.build();
    }
}
