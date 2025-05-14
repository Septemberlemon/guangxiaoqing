package com.example.guangxiaoqing;

import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import com.example.guangxiaoqing.utils.ToastHelper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.guangxiaoqing.api.NetworkCallback;
import com.example.guangxiaoqing.api.NetworkService;
import com.example.guangxiaoqing.model.TokenResponse;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ChangePasswordActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextInputLayout tilCurrentPassword, tilNewPassword, tilConfirmPassword;
    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private Button btnChangePassword;
    private UserSession userSession;
    private NetworkService networkService;
    private boolean isCurrentPasswordVisible = false;
    private boolean isNewPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // 隐藏ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // 初始化UserSession和NetworkService
        userSession = UserSession.getInstance(this);
        networkService = new NetworkService(this);

        initViews();
        setListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tilCurrentPassword = findViewById(R.id.tilCurrentPassword);
        tilNewPassword = findViewById(R.id.tilNewPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        // 设置当前密码可见性切换图标点击事件
        tilCurrentPassword.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleCurrentPasswordVisibility();
            }
        });

        // 设置新密码可见性切换图标点击事件
        tilNewPassword.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleNewPasswordVisibility();
            }
        });

        // 设置确认密码可见性切换图标点击事件
        tilConfirmPassword.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleConfirmPasswordVisibility();
            }
        });
    }

    private void toggleCurrentPasswordVisibility() {
        isCurrentPasswordVisible = !isCurrentPasswordVisible;

        if (isCurrentPasswordVisible) {
            // 显示密码
            etCurrentPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            tilCurrentPassword.setEndIconDrawable(R.drawable.ic_visibility);
        } else {
            // 隐藏密码
            etCurrentPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            tilCurrentPassword.setEndIconDrawable(R.drawable.ic_visibility_off);
        }

        // 保持光标位置在文本末尾
        etCurrentPassword.setSelection(etCurrentPassword.getText().length());
    }

    private void toggleNewPasswordVisibility() {
        isNewPasswordVisible = !isNewPasswordVisible;

        if (isNewPasswordVisible) {
            // 显示密码
            etNewPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            tilNewPassword.setEndIconDrawable(R.drawable.ic_visibility);
        } else {
            // 隐藏密码
            etNewPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            tilNewPassword.setEndIconDrawable(R.drawable.ic_visibility_off);
        }

        // 保持光标位置在文本末尾
        etNewPassword.setSelection(etNewPassword.getText().length());
    }

    private void toggleConfirmPasswordVisibility() {
        isConfirmPasswordVisible = !isConfirmPasswordVisible;

        if (isConfirmPasswordVisible) {
            // 显示确认密码
            etConfirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            tilConfirmPassword.setEndIconDrawable(R.drawable.ic_visibility);
        } else {
            // 隐藏确认密码
            etConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            tilConfirmPassword.setEndIconDrawable(R.drawable.ic_visibility_off);
        }

        // 保持光标位置在文本末尾
        etConfirmPassword.setSelection(etConfirmPassword.getText().length());
    }

    private void setListeners() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInputs()) {
                    changePassword();
                }
            }
        });
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // 获取所有输入内容
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // 检查是否所有输入框都为空
        if (TextUtils.isEmpty(currentPassword) && TextUtils.isEmpty(newPassword) &&
            TextUtils.isEmpty(confirmPassword)) {
            ToastHelper.showInfo(this, "请填写密码修改信息");
            return false;
        }

        // 验证当前密码
        if (TextUtils.isEmpty(currentPassword)) {
            ToastHelper.showInfo(this, "请输入当前密码");
            return false; // 当前密码为空时直接返回
        }

        // 验证新密码
        if (TextUtils.isEmpty(newPassword)) {
            ToastHelper.showInfo(this, "请输入新密码");
            return false; // 新密码为空时直接返回
        } else if (newPassword.length() < 6) {
            ToastHelper.showInfo(this, "密码长度至少为6位");
            return false;
        }

        // 验证确认密码
        if (TextUtils.isEmpty(confirmPassword)) {
            ToastHelper.showInfo(this, "请确认新密码");
            return false; // 确认密码为空时直接返回
        } else if (!confirmPassword.equals(newPassword)) {
            ToastHelper.showInfo(this, "两次输入的密码不一致");
            return false;
        }

        return isValid;
    }

    private void changePassword() {
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String phone = userSession.getUserPhone(); // 获取当前登录用户的手机号

        if (phone == null || phone.isEmpty()) {
            Toast.makeText(this, "登录状态异常，请重新登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 禁用按钮，防止重复提交
        btnChangePassword.setEnabled(false);

        // 调用修改密码API
        networkService.changePassword(phone, currentPassword, newPassword, new NetworkCallback<TokenResponse>() {
            @Override
            public void onSuccess(TokenResponse response) {
                // 修改密码成功，更新token
                userSession.saveLoginSession(phone, response.getAccessToken());

                // 显示成功提示
                ToastHelper.showSuccess(ChangePasswordActivity.this, "密码修改成功");

                // 添加延迟，确保用户能看到成功提示
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // 关闭当前页面
                        finish();
                    }
                }, 1500); // 延迟1.5秒后跳转，与注册和忘记密码界面保持一致
            }

            @Override
            public void onError(String errorMessage) {
                // 显示错误信息
                ToastHelper.showError(ChangePasswordActivity.this, errorMessage);
                btnChangePassword.setEnabled(true);
            }
        });
    }
}
