package com.example.guangxiaoqing.api;

import com.example.guangxiaoqing.model.ChatRequest;
import com.example.guangxiaoqing.model.ChatResponse;
import com.example.guangxiaoqing.model.LoginRequest;
import com.example.guangxiaoqing.model.PasswordChangeRequest;
import com.example.guangxiaoqing.model.RegisterRequest;
import com.example.guangxiaoqing.model.ResetPasswordRequest;
import com.example.guangxiaoqing.model.SmsRequest;
import com.example.guangxiaoqing.model.TokenResponse;
import com.example.guangxiaoqing.model.VerifyCodeRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Streaming;

/**
 * 后端API接口定义
 */
public interface ApiService {

    /**
     * 发送短信验证码
     * @param request 短信请求体
     * @return 响应结果
     */
    @POST("sms/send")
    Call<Object> sendSms(@Body SmsRequest request);

    /**
     * 验证短信验证码
     * @param request 验证码验证请求体
     * @return 响应结果
     */
    @POST("sms")
    Call<Object> verifyCode(@Body VerifyCodeRequest request);

    /**
     * 用户注册
     * @param request 注册请求体
     * @return Token响应
     */
    @POST("users")
    Call<TokenResponse> register(@Body RegisterRequest request);

    /**
     * 用户登录
     * @param request 登录请求体
     * @return Token响应
     */
    @POST("login")
    Call<TokenResponse> login(@Body LoginRequest request);

    /**
     * 重置密码（不需要token的密码重置）
     * @param request 密码重置请求体
     * @return Token响应
     */
    @POST("users/reset-password")
    Call<TokenResponse> resetPassword(@Body ResetPasswordRequest request);

    /**
     * 修改密码（需要token的密码修改）
     * @param token 认证Token
     * @param request 密码修改请求体
     * @return Token响应
     */
    @PUT("users/password")
    Call<TokenResponse> changePassword(
            @Header("Authorization") String token,
            @Body PasswordChangeRequest request);

    /**
     * 聊天接口
     * @param token 认证Token
     * @param request 聊天请求对象
     * @return 聊天响应
     */
    @Streaming
    @POST("chat/stream")
    Call<ResponseBody> chat(@Header("Authorization") String authHeader, @Body ChatRequest request);
}