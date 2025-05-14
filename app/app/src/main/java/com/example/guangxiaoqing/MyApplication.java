package com.example.guangxiaoqing;

import android.app.Application;
import android.util.Log;
import androidx.appcompat.app.AppCompatDelegate;

public class MyApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 强制使用浅色模式
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        
        // 在这里设置全局主题，避免XML样式引用问题
        try {
            // 获取SimpleTheme的资源ID
            int themeId = getResources().getIdentifier(
                "SimpleTheme", "style", getPackageName());
            
            if (themeId != 0) {
                // 设置应用程序主题
                setTheme(themeId);
                Log.d("MyApplication", "Successfully set SimpleTheme");
            } else {
                Log.e("MyApplication", "Failed to find SimpleTheme");
            }
        } catch (Exception e) {
            Log.e("MyApplication", "Error setting theme: " + e.getMessage());
        }
    }
} 
