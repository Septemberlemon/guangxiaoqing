package com.example.guangxiaoqing;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 启动页面，检查登录状态并跳转到相应界面
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 1000; // 1秒延迟
    private UserSession userSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        // 初始化UserSession
        userSession = UserSession.getInstance(this);
        
        // 延迟跳转到相应界面
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // 检查是否已登录
                if (userSession.isLoggedIn()) {
                    // 已登录，直接进入聊天界面
                    startActivity(new Intent(SplashActivity.this, ChatActivity.class));
                } else {
                    // 未登录，跳转到登录界面
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                }
                finish();
            }
        }, SPLASH_DELAY);
    }
} 