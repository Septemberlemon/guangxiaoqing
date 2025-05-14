package com.example.guangxiaoqing.api;
import java.io.IOException;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.guangxiaoqing.UserSession;
import com.example.guangxiaoqing.model.ChatMessage;
import com.example.guangxiaoqing.model.ChatRequest;
import com.example.guangxiaoqing.model.ChatResponse;
import com.example.guangxiaoqing.model.LoginRequest;
import com.example.guangxiaoqing.model.PasswordChangeRequest;
import com.example.guangxiaoqing.model.RegisterRequest;
import com.example.guangxiaoqing.model.ResetPasswordRequest;
import com.example.guangxiaoqing.model.SmsRequest;
import com.example.guangxiaoqing.model.TokenResponse;
import com.example.guangxiaoqing.model.VerifyCodeRequest;
import com.example.guangxiaoqing.utils.ToastHelper;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;
import java.util.ArrayList;
import java.net.SocketTimeoutException;

/**
 * 网络服务管理类，封装API调用逻辑
 */
public class NetworkService {
    private static final String TAG = "NetworkService";
    private final Context context;
    private final ApiService apiService;
    private final UserSession userSession;
    private final Handler mainExecutor = new Handler(Looper.getMainLooper());

    public NetworkService(Context context) {
        this.context = context;
        this.apiService = ApiClient.getApiService(context);
        this.userSession = UserSession.getInstance(context);
    }

    /**
     * 发送短信验证码
     * @param phone 手机号
     * @param type 类型（注册或重置密码）
     * @param callback 回调接口
     */
    public void sendSms(String phone, String type, final NetworkCallback<Object> callback) {
        SmsRequest request = new SmsRequest(phone, type);
        Log.d(TAG, "发送验证码请求: 手机号=" + phone + ", 类型=" + type + ", URL=" + ApiClient.BASE_URL);

        // 设置较长的超时时间，特别是对验证码请求
        Call<Object> call = apiService.sendSms(request);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                Log.d(TAG, "验证码请求响应: " + response.code() + ", URL=" + call.request().url());
                if (response.isSuccessful()) {
                    Log.d(TAG, "验证码请求成功: " + response.body());
                    callback.onSuccess(response.body());
                } else {
                    String errorMsg = "发送验证码失败: " + response.code();
                    try {
                        // 尝试解析错误信息
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析错误信息失败", e);
                    }
                    Log.e(TAG, errorMsg);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                String errorMsg = "网络请求失败: " + t.getMessage();
                Log.e(TAG, "发送验证码失败: " + errorMsg, t);
                // 添加更多日志信息，帮助调试
                Log.e(TAG, "请求URL: " + call.request().url());
                Log.e(TAG, "异常类型: " + t.getClass().getName());
                t.printStackTrace();
                callback.onError(errorMsg);
            }
        });
    }

    /**
     * 用户注册
     * @param phone 手机号
     * @param password 密码
     * @param code 验证码
     * @param callback 回调接口
     */
    public void register(String phone, String password, String code, final NetworkCallback<TokenResponse> callback) {
        RegisterRequest request = new RegisterRequest(phone, password, code);
        Log.d(TAG, "发送注册请求: 手机号=" + phone + ", 验证码=" + code + ", URL=" + ApiClient.BASE_URL);
        apiService.register(request).enqueue(new Callback<TokenResponse>() {
            @Override
            public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                Log.d(TAG, "注册请求响应: " + response.code() + ", URL=" + call.request().url());
                if (response.isSuccessful() && response.body() != null) {
                    TokenResponse tokenResponse = response.body();
                    Log.d(TAG, "注册成功: token=" + tokenResponse.getAccessToken());
                    // 保存登录状态和token
                    userSession.saveLoginSession(phone, tokenResponse.getAccessToken());
                    callback.onSuccess(tokenResponse);
                } else {
                    String errorMsg = "注册失败: " + response.code();
                    try {
                        // 尝试解析错误信息
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "错误响应体: " + errorBody);

                            // 尝试提取更友好的错误消息
                            if (errorBody.contains("验证码错误") || errorBody.contains("验证码已过期")) {
                                errorMsg = "验证码错误或已过期，请重新获取";
                            } else if (errorBody.contains("该手机号已注册")) {
                                errorMsg = "该手机号已注册";
                            } else if (errorBody.contains("未找到有效的验证码记录")) {
                                errorMsg = "未找到有效的验证码，请重新获取";
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析错误信息失败", e);
                    }
                    Log.e(TAG, errorMsg);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<TokenResponse> call, Throwable t) {
                String errorMsg = "网络请求失败: " + t.getMessage();
                Log.e(TAG, "注册失败: " + errorMsg, t);
                callback.onError(errorMsg);
            }
        });
    }

    /**
     * 用户登录
     * @param phone 手机号
     * @param password 密码
     * @param callback 回调接口
     */
    public void login(String phone, String password, final NetworkCallback<TokenResponse> callback) {
        LoginRequest request = new LoginRequest(phone, password);
        Log.d(TAG, "发送登录请求: 手机号=" + phone + ", URL=" + ApiClient.BASE_URL);
        apiService.login(request).enqueue(new Callback<TokenResponse>() {
            @Override
            public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                Log.d(TAG, "登录请求响应: " + response.code() + ", URL=" + call.request().url());
                if (response.isSuccessful() && response.body() != null) {
                    TokenResponse tokenResponse = response.body();
                    Log.d(TAG, "登录成功: token=" + tokenResponse.getAccessToken());
                    // 保存登录状态和token
                    userSession.saveLoginSession(phone, tokenResponse.getAccessToken());
                    callback.onSuccess(tokenResponse);
                } else {
                    String errorMsg = "登录失败: " + response.code();
                    Log.e(TAG, errorMsg);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<TokenResponse> call, Throwable t) {
                String errorMsg = "网络请求失败: " + t.getMessage();
                Log.e(TAG, "登录失败: " + errorMsg, t);
                callback.onError(errorMsg);
            }
        });
    }

    /**
     * 验证验证码
     * @param phone 手机号
     * @param code 验证码
     * @param type 类型（注册或重置密码）
     * @param callback 回调接口
     */
    public void verifyCode(String phone, String code, String type, final NetworkCallback<Object> callback) {
        VerifyCodeRequest request = new VerifyCodeRequest(phone, code, type);
        Log.d(TAG, "发送验证码验证请求: 手机号=" + phone + ", 类型=" + type + ", URL=" + ApiClient.BASE_URL);

        apiService.verifyCode(request).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                Log.d(TAG, "验证码验证响应: " + response.code() + ", URL=" + call.request().url());
                if (response.isSuccessful()) {
                    Log.d(TAG, "验证码验证成功: " + response.body());
                    callback.onSuccess(response.body());
                } else {
                    String errorMsg = "验证码验证失败: " + response.code();
                    try {
                        // 尝试解析错误信息
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析错误信息失败", e);
                    }
                    Log.e(TAG, errorMsg);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                String errorMsg = "网络请求失败: " + t.getMessage();
                Log.e(TAG, "验证码验证失败: " + errorMsg, t);
                callback.onError(errorMsg);
            }
        });
    }

    /**
     * 重置密码（不需要token的密码重置）
     * @param phone 手机号
     * @param code 验证码
     * @param newPassword 新密码
     * @param callback 回调接口
     */
    public void resetPassword(String phone, String code, String newPassword, final NetworkCallback<TokenResponse> callback) {
        ResetPasswordRequest request = new ResetPasswordRequest(phone, code, newPassword);
        Log.d(TAG, "发送密码重置请求: 手机号=" + phone + ", URL=" + ApiClient.BASE_URL);

        apiService.resetPassword(request).enqueue(new Callback<TokenResponse>() {
            @Override
            public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                Log.d(TAG, "密码重置响应: " + response.code() + ", URL=" + call.request().url());
                if (response.isSuccessful() && response.body() != null) {
                    TokenResponse tokenResponse = response.body();
                    Log.d(TAG, "密码重置成功: token=" + tokenResponse.getAccessToken());
                    // 保存登录状态和token
                    userSession.saveLoginSession(phone, tokenResponse.getAccessToken());
                    callback.onSuccess(tokenResponse);
                } else {
                    String errorMsg = "密码重置失败: " + response.code();
                    try {
                        // 尝试解析错误信息
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "错误响应体: " + errorBody);

                            // 尝试提取更友好的错误消息
                            if (errorBody.contains("验证码错误") || errorBody.contains("验证码已过期")) {
                                errorMsg = "验证码错误或已过期，请重新获取";
                            } else if (errorBody.contains("该手机号未注册")) {
                                errorMsg = "该手机号未注册";
                            } else if (errorBody.contains("未找到有效的验证码记录")) {
                                errorMsg = "未找到有效的验证码，请重新获取";
                            } else if (errorBody.contains("用户不存在")) {
                                errorMsg = "该手机号未注册";
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析错误信息失败", e);
                    }
                    Log.e(TAG, errorMsg);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<TokenResponse> call, Throwable t) {
                String errorMsg = "网络请求失败: " + t.getMessage();
                Log.e(TAG, "密码重置失败: " + errorMsg, t);
                callback.onError(errorMsg);
            }
        });
    }

    /**
     * 修改密码（需要token的密码修改）
     * @param phone 手机号
     * @param oldPassword 原密码
     * @param newPassword 新密码
     * @param callback 回调接口
     */
    public void changePassword(String phone, String oldPassword, String newPassword, final NetworkCallback<TokenResponse> callback) {
        String authHeader = ApiClient.getAuthHeader(context);
        if (authHeader == null) {
            Log.e(TAG, "修改密码失败: 未登录，请先登录");
            callback.onError("未登录，请先登录");
            return;
        }

        PasswordChangeRequest request = new PasswordChangeRequest(phone, oldPassword, newPassword);
        Log.d(TAG, "发送修改密码请求: 手机号=" + phone + ", URL=" + ApiClient.BASE_URL);
        apiService.changePassword(authHeader, request).enqueue(new Callback<TokenResponse>() {
            @Override
            public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                Log.d(TAG, "修改密码请求响应: " + response.code() + ", URL=" + call.request().url());
                if (response.isSuccessful() && response.body() != null) {
                    TokenResponse tokenResponse = response.body();
                    Log.d(TAG, "修改密码成功: token=" + tokenResponse.getAccessToken());
                    // 更新token
                    userSession.saveLoginSession(phone, tokenResponse.getAccessToken());
                    callback.onSuccess(tokenResponse);
                } else {
                    String errorMsg = "修改密码失败: " + response.code();
                    // 处理token失效情况
                    if (response.code() == 401) {
                        userSession.clearLoginSession();
                        errorMsg = "登录已过期，请重新登录";
                    }
                    try {
                        // 尝试解析错误信息
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析错误信息失败", e);
                    }
                    Log.e(TAG, errorMsg);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<TokenResponse> call, Throwable t) {
                String errorMsg = "网络请求失败: " + t.getMessage();
                Log.e(TAG, "修改密码失败: " + errorMsg, t);
                callback.onError(errorMsg);
            }
        });
    }

    /**
     * 聊天
     * @param message 消息内容
     * @param callback 回调接口
     */
    public void chat(String message, final NetworkCallback<String> callback) {
        // 使用空的历史记录列表调用chatWithHistory方法
        // 这样可以复用chatWithHistory的流式处理逻辑
        chatWithHistory(message, new ArrayList<>(), callback);
    }

    /**
     * 聊天（支持历史消息）
     * @param message 当前消息内容
     * @param history 历史消息列表
     * @param callback 回调接口
     */
    public void chatWithHistory(String message, List<ChatMessage> history, final NetworkCallback<String> callback) {
        // 调用带重试次数的方法，默认最多重试2次
        chatWithHistoryAndRetry(message, history, callback, 2);
    }

    /**
     * 聊天（支持历史消息和重试机制）
     * @param message 当前消息内容
     * @param history 历史消息列表
     * @param callback 回调接口
     * @param retryCount 剩余重试次数
     */
    private void chatWithHistoryAndRetry(String message, List<ChatMessage> history, final NetworkCallback<String> callback, int retryCount) {
        String authHeader = ApiClient.getAuthHeader(context);
        if (authHeader == null) {
            callback.onError("未登录，请先登录");
            return;
        }

        // 使用ChatRequest对象包装消息内容和历史
        ChatRequest chatRequest = new ChatRequest(message, history);
        Log.d(TAG, "发送聊天请求: message=" + message + ", history=" + (history != null ? history.size() : 0) + "条, 剩余重试次数=" + retryCount);

        apiService.chat(authHeader, chatRequest).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.d(TAG, "收到聊天响应: code=" + response.code() + ", contentType=" + response.headers().get("Content-Type") + ", contentLength=" + response.headers().get("Content-Length"));
                // 打印请求信息，帮助诊断
                Log.d(TAG, "请求URL: " + call.request().url() + ", 请求方法: " + call.request().method());
                if (response.isSuccessful()) {
                    try {
                        if (response.body() != null) {
                            // 使用BufferedSource读取响应
                            ResponseBody responseBody = response.body();
                            Log.d(TAG, "响应体类型: " + responseBody.contentType() + ", 响应体长度: " + responseBody.contentLength());

                            okio.BufferedSource source = responseBody.source();

                            // 添加日志确认响应体开始读取
                            Log.d(TAG, "开始读取流式响应体");

                            // 创建一个新线程来处理流式响应
                            new Thread(() -> {
                                try {
                                    Log.d(TAG, "流式响应线程已启动");

                                    // 确保连接保持打开状态
                                    responseBody.contentType(); // 触发一次读取，确保连接已建立

                                    // 强制请求服务器发送数据
                                    source.request(1);

                                    Log.d(TAG, "source.exhausted()状态: " + source.exhausted());

                                    // 如果source.exhausted()为true，尝试再次请求数据
                                    if (source.exhausted()) {
                                        Log.d(TAG, "尝试再次请求数据...");
                                        source.request(Long.MAX_VALUE); // 请求尽可能多的数据
                                        Log.d(TAG, "再次请求后source.exhausted()状态: " + source.exhausted());
                                    }

                                    int charCount = 0;
                                    StringBuilder buffer = new StringBuilder();

                                    // 使用超时机制，避免无限等待
                                    long startTime = System.currentTimeMillis();
                                    long timeout = 180000; // 增加到180秒(3分钟)超时
                                    long maxWaitTime = 30000; // 增加到30秒的最大等待时间

                                    // 使用更简单的读取逻辑，避免无限循环
                                    boolean isComplete = false;
                                    int emptyReadCount = 0; // 计数器，记录连续空读取次数
                                    final int MAX_EMPTY_READS = 3; // 最大允许的连续空读取次数

                                    // 记录请求ID，用于日志跟踪
                                    String requestId = String.format("%08x", System.currentTimeMillis() & 0xFFFFFFFF);
                                    Log.d(TAG, "[" + requestId + "] 开始读取流式响应");

                                    // 检查响应头信息
                                    String contentType = responseBody.contentType() != null ? responseBody.contentType().toString() : "unknown";
                                    String transferEncoding = response.headers().get("Transfer-Encoding");
                                    String connection = response.headers().get("Connection");
                                    Log.d(TAG, "[" + requestId + "] 响应头信息: Content-Type=" + contentType
                                            + ", Transfer-Encoding=" + transferEncoding
                                            + ", Connection=" + connection);

                                    // 设置读取超时时间
                                    long readTimeout = 15000; // 15秒读取超时
                                    long lastReadTime = System.currentTimeMillis();

                                    while (!isComplete) {
                                        try {
                                            // 检查是否超时（总体超时）
                                            if (System.currentTimeMillis() - startTime > timeout) {
                                                Log.d(TAG, "[" + requestId + "] 总体读取超时，结束读取");
                                                isComplete = true;
                                                break;
                                            }

                                            // 检查读取超时（两次读取之间的时间）
                                            if (System.currentTimeMillis() - lastReadTime > readTimeout) {
                                                Log.d(TAG, "[" + requestId + "] 读取超时，尝试再次请求数据");
                                                source.request(Long.MAX_VALUE);

                                                // 如果仍然没有数据，增加空读取计数
                                                if (source.exhausted()) {
                                                    emptyReadCount++;
                                                    Log.d(TAG, "[" + requestId + "] 读取超时后仍无数据，空读取计数: " + emptyReadCount);

                                                    if (emptyReadCount >= MAX_EMPTY_READS) {
                                                        Log.d(TAG, "[" + requestId + "] 达到最大空读取次数，结束读取");
                                                        isComplete = true;
                                                        break;
                                                    }

                                                    // 短暂等待后继续
                                                    Thread.sleep(100);
                                                    continue;
                                                }
                                            }

                                            // 如果source已耗尽，检查是否有更多数据
                                            if (source.exhausted()) {
                                                // 检查是否有分块传输结束标记
                                                // 注意：这是一个简化的检查，实际上应该解析HTTP分块传输编码
                                                byte[] peekBuffer = new byte[5];  // 改名为peekBuffer避免命名冲突
                                                int bytesRead = 0;
                                                try {
                                                    // 尝试读取可能的结束标记
                                                    bytesRead = source.peek().read(peekBuffer, 0, 5);
                                                    if (bytesRead > 0) {
                                                        String peek = new String(peekBuffer, 0, bytesRead);
                                                        if (peek.contains("0\r\n") || peek.equals("\r\n0\r")) {
                                                            Log.d(TAG, "[" + requestId + "] 检测到分块传输结束标记");
                                                            isComplete = true;
                                                            break;
                                                        }
                                                    }
                                                } catch (Exception e) {
                                                    // 忽略peek异常
                                                }

                                                // 增加空读取计数
                                                emptyReadCount++;
                                                Log.d(TAG, "[" + requestId + "] source已耗尽，尝试读取更多数据... (尝试次数: " + emptyReadCount + ")");

                                                // 如果连续多次读取都为空，认为传输结束
                                                if (emptyReadCount >= MAX_EMPTY_READS) {
                                                    Log.d(TAG, "[" + requestId + "] 连续" + MAX_EMPTY_READS + "次空读取，认为数据传输已完成");
                                                    isComplete = true;
                                                    break;
                                                }

                                                // 尝试读取更多数据
                                                source.request(Long.MAX_VALUE);
                                                Thread.sleep(100); // 短暂等待，避免CPU占用过高
                                                continue;
                                            }

                                            // 重置空读取计数和最后读取时间
                                            emptyReadCount = 0;
                                            lastReadTime = System.currentTimeMillis();

                                            // 读取一个UTF-8字符
                                            int codePoint = source.readUtf8CodePoint();
                                            if (codePoint != -1) {
                                                charCount++;
                                                // 将代码点转换为字符串
                                                String character = new String(Character.toChars(codePoint));
                                                buffer.append(character);

                                                // 每积累5个字符或遇到换行符就发送一次（减少积累字符数，提高响应速度）
                                                if (buffer.length() >= 5 || character.equals("\n")) {
                                                    final String textToSend = buffer.toString();
                                                    // 在主线程中更新UI
                                                    mainExecutor.post(() -> {
                                                        callback.onSuccess(textToSend);
                                                    });
                                                    buffer.setLength(0); // 清空缓冲区
                                                }

                                                // 每接收20个字符记录一次日志（减少日志频率）
                                                if (charCount % 20 == 0) {
                                                    Log.d(TAG, "已接收" + charCount + "个字符");
                                                }

                                                // 重置超时计时器
                                                startTime = System.currentTimeMillis();
                                            }
                                        } catch (IOException e) {
                                            Log.e(TAG, "读取字符时发生IO异常", e);

                                            // 如果还有重试次数，尝试重新连接
                                            if (retryCount > 0) {
                                                Log.d(TAG, "IO异常，准备重试...");
                                                mainExecutor.post(() -> {
                                                    // 通知用户正在重试
                                                    callback.onSuccess("\n[连接中断，正在重试...]\n");
                                                    // 延迟1秒后重试，避免立即重试可能导致的连续失败
                                                    new Handler().postDelayed(() -> {
                                                        chatWithHistoryAndRetry(message, history, callback, retryCount - 1);
                                                    }, 1000);
                                                });
                                                return; // 结束当前线程
                                            }

                                            break;
                                        }
                                    }

                                    // 发送剩余的字符
                                    if (buffer.length() > 0) {
                                        final String textToSend = buffer.toString();
                                        mainExecutor.post(() -> {
                                            callback.onSuccess(textToSend);
                                        });
                                    }

                                    Log.d(TAG, "流式响应读取完成，总共接收" + charCount + "个字符");
                                } catch (Exception e) {
                                    Log.e(TAG, "读取响应失败", e);

                                    // 如果还有重试次数，尝试重新连接
                                    if (retryCount > 0) {
                                        Log.d(TAG, "读取失败，准备重试...");
                                        mainExecutor.post(() -> {
                                            // 通知用户正在重试
                                            callback.onSuccess("\n[连接异常，正在重试...]\n");
                                            // 延迟1秒后重试，避免立即重试可能导致的连续失败
                                            new Handler().postDelayed(() -> {
                                                chatWithHistoryAndRetry(message, history, callback, retryCount - 1);
                                            }, 1000);
                                        });
                                    } else {
                                        mainExecutor.post(() -> {
                                            callback.onError("读取响应失败: " + e.getMessage());
                                        });
                                    }
                                }
                            }).start();
                        } else {
                            String errorMsg = "响应体为空";
                            Log.e(TAG, errorMsg);

                            // 如果还有重试次数，尝试重新连接
                            if (retryCount > 0) {
                                Log.d(TAG, "响应体为空，准备重试...");
                                // 延迟1秒后重试，避免立即重试可能导致的连续失败
                                new Handler().postDelayed(() -> {
                                    chatWithHistoryAndRetry(message, history, callback, retryCount - 1);
                                }, 1000);
                            } else {
                                callback.onError(errorMsg);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析响应失败", e);

                        // 如果还有重试次数，尝试重新连接
                        if (retryCount > 0) {
                            Log.d(TAG, "解析失败，准备重试...");
                            // 延迟1秒后重试，避免立即重试可能导致的连续失败
                            new Handler().postDelayed(() -> {
                                chatWithHistoryAndRetry(message, history, callback, retryCount - 1);
                            }, 1000);
                        } else {
                            callback.onError("解析响应失败: " + e.getMessage());
                        }
                    }
                } else {
                    String errorMsg = "聊天请求失败: " + response.code();
                    Log.e(TAG, errorMsg);

                    // 处理token失效情况
                    if (response.code() == 401) {
                        userSession.clearLoginSession();
                        errorMsg = "登录已过期，请重新登录";
                        callback.onError(errorMsg);
                        return; // 登录过期不需要重试
                    }

                    try {
                        // 尝试解析错误信息
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "错误响应体: " + errorBody);
                            errorMsg = errorBody;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析错误信息失败", e);
                    }

                    // 对于服务器错误(5xx)，如果还有重试次数，尝试重试
                    if (response.code() >= 500 && retryCount > 0) {
                        Log.d(TAG, "服务器错误，准备重试...");
                        // 延迟2秒后重试，给服务器一些恢复时间
                        new Handler().postDelayed(() -> {
                            chatWithHistoryAndRetry(message, history, callback, retryCount - 1);
                        }, 2000);
                    } else {
                        callback.onError(errorMsg);
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // 添加更详细的错误日志
                Log.e(TAG, "聊天请求失败", t);
                Log.e(TAG, "请求URL: " + call.request().url());

                // 如果还有重试次数，尝试重新连接
                if (retryCount > 0) {
                    Log.d(TAG, "网络请求失败，准备重试...");

                    // 根据错误类型决定重试延迟时间
                    int delayMillis = 1000; // 默认1秒
                    if (t instanceof SocketTimeoutException) {
                        delayMillis = 3000; // 超时错误延迟3秒
                    }

                    // 延迟后重试
                    final int finalDelayMillis = delayMillis;
                    new Handler().postDelayed(() -> {
                        chatWithHistoryAndRetry(message, history, callback, retryCount - 1);
                    }, finalDelayMillis);
                } else {
                    if (t instanceof SocketTimeoutException) {
                        // 对于超时错误，提供更友好的错误信息
                        callback.onError("请求超时，服务器响应时间过长，请稍后再试");
                    } else {
                        callback.onError("网络请求失败: " + t.getMessage());
                    }
                }
            }
        });
    }
}
