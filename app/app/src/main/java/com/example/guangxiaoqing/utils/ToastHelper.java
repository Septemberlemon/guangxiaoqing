package com.example.guangxiaoqing.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.example.guangxiaoqing.R;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自定义Toast工具类，提供更友好的消息提示
 */
public class ToastHelper {
    private static final int DEFAULT_DURATION = 2000; // 默认显示时间2秒
    private static final int ERROR_DURATION = 3000;   // 错误消息显示时间3秒

    private static PopupWindow currentToast;
    private static Handler handler = new Handler();
    private static Runnable dismissRunnable;

    // 用于存储不同类型错误的友好提示
    private static final Map<String, String> ERROR_MESSAGES = new HashMap<>();

    static {
        // 初始化错误消息映射
        ERROR_MESSAGES.put("该手机号已注册", "该手机号已注册，请直接登录");
        ERROR_MESSAGES.put("该手机号未注册", "该手机号未注册，请先注册");
        ERROR_MESSAGES.put("手机号或密码错误", "手机号或密码错误，请重试");
        ERROR_MESSAGES.put("验证码错误", "验证码错误，请重新输入");
        ERROR_MESSAGES.put("验证码已过期", "验证码已过期，请重新获取");
        ERROR_MESSAGES.put("网络请求失败", "网络连接失败，请检查网络设置");
        ERROR_MESSAGES.put("登录已过期", "登录已过期，请重新登录");
        ERROR_MESSAGES.put("超过限制", "短信发送次数已达上限");

        // 添加密码相关的错误消息
        ERROR_MESSAGES.put("422", "密码不符合要求，应为6-20位字符");
        ERROR_MESSAGES.put("401", "手机号或密码错误");
        ERROR_MESSAGES.put("密码错误", "密码错误，请重新输入");
        ERROR_MESSAGES.put("密码长度", "密码长度不符合要求，应为6-20位字符");
        ERROR_MESSAGES.put("密码弱", "密码弱，请使用字母、数字的组合");
    }

    /**
     * 显示成功消息
     * @param context 上下文
     * @param message 消息内容
     */
    public static void showSuccess(Context context, String message) {
        show(context, message, R.drawable.ic_success, DEFAULT_DURATION);
    }

    /**
     * 显示错误消息
     * @param context 上下文
     * @param message 错误消息
     */
    public static void showError(Context context, String message) {
        // 处理错误消息，提取友好的提示
        String friendlyMessage = getFriendlyErrorMessage(message);
        show(context, friendlyMessage, R.drawable.ic_error, ERROR_DURATION);
    }

    /**
     * 显示信息消息
     * @param context 上下文
     * @param message 消息内容
     */
    public static void showInfo(Context context, String message) {
        show(context, message, R.drawable.ic_info, DEFAULT_DURATION);
    }

    /**
     * 显示自定义消息
     * @param context 上下文
     * @param message 消息内容
     * @param iconResId 图标资源ID
     * @param duration 显示时间
     */
    private static void show(Context context, String message, int iconResId, int duration) {
        if (context == null || !(context instanceof Activity)) {
            return;
        }

        final Activity activity = (Activity) context;

        // 如果当前有Toast在显示，先取消
        dismiss();

        // 在UI线程中创建和显示Toast
        activity.runOnUiThread(() -> {
            try {
                // 获取布局
                View toastView = LayoutInflater.from(context).inflate(R.layout.custom_toast, null);
                TextView tvMessage = toastView.findViewById(R.id.tvToastMessage);

                // 设置消息
                tvMessage.setText(message);

                // 创建PopupWindow
                currentToast = new PopupWindow(
                        toastView,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

                // 设置动画和背景
                currentToast.setAnimationStyle(R.style.ToastAnimation);
                currentToast.setBackgroundDrawable(null);
                currentToast.setOutsideTouchable(true);

                // 计算位置 - 显示在底部中间，不遮挡键盘
                View rootView = activity.getWindow().getDecorView().findViewById(android.R.id.content);
                int yOffset = rootView.getHeight() / 4; // 距离底部1/4的位置

                // 显示Toast
                currentToast.showAtLocation(rootView, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, yOffset);

                // 设置定时关闭
                dismissRunnable = () -> dismiss();
                handler.postDelayed(dismissRunnable, duration);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 关闭当前显示的Toast
     */
    public static void dismiss() {
        if (currentToast != null && currentToast.isShowing()) {
            currentToast.dismiss();
            currentToast = null;
        }

        if (handler != null && dismissRunnable != null) {
            handler.removeCallbacks(dismissRunnable);
            dismissRunnable = null;
        }
    }

    /**
     * 从错误消息中提取友好的提示
     * @param errorMessage 原始错误消息
     * @return 友好的错误提示
     */
    private static String getFriendlyErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isEmpty()) {
            return "操作失败，请稍后重试";
        }

        // 处理JSON格式的错误消息
        if (errorMessage.contains("{\"detail\":")) {
            Pattern pattern = Pattern.compile("\\{\"detail\":\"(.*?)\"\\}");
            Matcher matcher = pattern.matcher(errorMessage);
            if (matcher.find()) {
                errorMessage = matcher.group(1);
            }
        }

        // 检查是否包含已知的错误关键词
        for (Map.Entry<String, String> entry : ERROR_MESSAGES.entrySet()) {
            if (errorMessage.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // 处理短信发送限制的特殊情况
        if (errorMessage.contains("过去30天内已发送") && errorMessage.contains("次验证码")) {
            return "短信发送次数已达上限，请30天后再试";
        }

        // 如果是网络错误
        if (errorMessage.contains("Failed to connect") ||
            errorMessage.contains("timeout") ||
            errorMessage.contains("网络请求失败")) {
            return "网络连接失败，请检查网络设置";
        }

        // 如果是服务器错误
        if (errorMessage.contains("500") || errorMessage.contains("服务器")) {
            return "服务器繁忙，请稍后重试";
        }

        // 如果是验证码相关错误
        if (errorMessage.contains("验证码")) {
            if (errorMessage.contains("错误")) {
                return "验证码错误，请重新输入";
            } else if (errorMessage.contains("过期")) {
                return "验证码已过期，请重新获取";
            }
        }

        // 如果是密码相关错误
        if (errorMessage.contains("密码")) {
            if (errorMessage.contains("错误")) {
                return "密码错误，请重新输入";
            } else if (errorMessage.contains("长度") || errorMessage.contains("不符合")) {
                return "密码长度不符合要求，应为6-20位字符";
            }
        }

        // 如果是登录失败的特殊情况
        if (errorMessage.contains("登录失败: 422") ||
            (errorMessage.contains("422") && !errorMessage.contains("密码"))) {
            return "密码不符合要求，应为6-20位字符";
        }

        if (errorMessage.contains("登录失败: 401") ||
            (errorMessage.contains("401") && !errorMessage.contains("登录已过期"))) {
            return "手机号或密码错误，请重试";
        }

        // 如果是登录相关错误，但不是密码错误
        if ((errorMessage.contains("未授权") || errorMessage.contains("token")) &&
            !errorMessage.contains("手机号或密码错误") &&
            !errorMessage.contains("密码错误")) {
            return "登录已过期，请重新登录";
        }

        // 默认情况，保留原始错误但限制长度
        if (errorMessage.length() > 30) {
            return errorMessage.substring(0, 27) + "...";
        }

        return errorMessage;
    }
}
