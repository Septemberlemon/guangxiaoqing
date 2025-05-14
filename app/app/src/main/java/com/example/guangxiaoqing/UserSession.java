package com.example.guangxiaoqing;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 用户会话管理类，用于保存和检查用户登录状态
 */
public class UserSession {
    private static final String PREF_NAME = "UserSessionPref";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_PHONE = "userPhone";
    private static final String KEY_TOKEN = "userToken";
    
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Context context;
    
    // 单例模式实例
    private static UserSession instance;
    
    private UserSession(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }
    
    /**
     * 获取UserSession实例
     */
    public static synchronized UserSession getInstance(Context context) {
        if (instance == null) {
            instance = new UserSession(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * 保存登录状态和用户信息
     */
    public void saveLoginSession(String phoneNumber, String token) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_PHONE, phoneNumber);
        editor.putString(KEY_TOKEN, token);
        editor.apply();
    }
    
    /**
     * 获取保存的token
     */
    public String getToken() {
        return sharedPreferences.getString(KEY_TOKEN, null);
    }
    
    /**
     * 清除登录状态
     */
    public void clearLoginSession() {
        editor.clear();
        editor.apply();
    }
    
    /**
     * 检查用户是否已登录
     */
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }
    
    /**
     * 获取已登录用户的手机号
     */
    public String getUserPhone() {
        return sharedPreferences.getString(KEY_USER_PHONE, "");
    }
}