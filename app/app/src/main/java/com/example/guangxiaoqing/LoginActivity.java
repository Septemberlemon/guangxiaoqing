package com.example.guangxiaoqing;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.example.guangxiaoqing.utils.ToastHelper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.example.guangxiaoqing.api.NetworkService;
import com.example.guangxiaoqing.model.TokenResponse;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilPhone, tilPassword;
    private TextInputEditText etPhone, etPassword;
    private Button btnLogin;
    private TextView tvForgetPassword, tvRegister;
    private boolean isPasswordVisible = false;
    private UserSession userSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化UserSession
        userSession = UserSession.getInstance(this);

        // 检查用户是否已登录
        if (userSession.isLoggedIn()) {
            // 已登录，直接进入聊天界面
            startActivity(new Intent(LoginActivity.this, ChatActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        initViews();
        setListeners();
    }

    private void initViews() {
        tilPhone = findViewById(R.id.tilPhone);
        tilPassword = findViewById(R.id.tilPassword);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgetPassword = findViewById(R.id.tvForgetPassword);
        tvRegister = findViewById(R.id.tvRegister);

        // 设置密码可见性切换图标点击事件
        tilPassword.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility();
            }
        });
    }

    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;

        if (isPasswordVisible) {
            // 显示密码
            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            tilPassword.setEndIconDrawable(R.drawable.ic_visibility);
        } else {
            // 隐藏密码
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            tilPassword.setEndIconDrawable(R.drawable.ic_visibility_off);
        }

        // 保持光标位置在文本末尾
        etPassword.setSelection(etPassword.getText().length());
    }

    private void setListeners() {
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInputs()) {
                    // 获取电话号码
                    String phoneNumber = etPhone.getText().toString().trim();
                    String password = etPassword.getText().toString().trim();

                    // 调用登录API
                    NetworkService networkService = new NetworkService(LoginActivity.this);
                    networkService.login(phoneNumber, password, new com.example.guangxiaoqing.api.NetworkCallback<TokenResponse>() {
                        @Override
                        public void onSuccess(TokenResponse response) {
                            // 登录成功后，保存登录状态和token
                            userSession.saveLoginSession(phoneNumber, response.getAccessToken());

                            // 跳转到聊天界面
                            Intent intent = new Intent(LoginActivity.this, ChatActivity.class);
                            startActivity(intent);
                            finish();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            // 显示错误信息
                            ToastHelper.showError(LoginActivity.this, errorMessage);
                        }
                    });
                }
            }
        });

        tvForgetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            }
        });

        tvRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    private boolean validateInputs() {
        boolean isValid = true;

        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // 如果两个都为空，显示综合提示
        if (TextUtils.isEmpty(phone) && TextUtils.isEmpty(password)) {
            ToastHelper.showInfo(this, "请输入手机号和密码");
            return false;
        }

        // 验证手机号
        if (TextUtils.isEmpty(phone)) {
            ToastHelper.showInfo(this, "请输入手机号");
            return false; // 手机号为空时直接返回，不再检查其他字段
        } else if (phone.length() != 11) {
            ToastHelper.showInfo(this, "请输入11位手机号");
            return false;
        }

        // 验证密码
        if (TextUtils.isEmpty(password)) {
            ToastHelper.showInfo(this, "请输入密码");
            return false;
        }

        return isValid;
    }
}