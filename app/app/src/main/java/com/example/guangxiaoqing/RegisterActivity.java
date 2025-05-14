package com.example.guangxiaoqing;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import com.example.guangxiaoqing.utils.ToastHelper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextInputLayout tilPhone, tilVerificationCode, tilPassword, tilConfirmPassword;
    private TextInputEditText etPhone, etVerificationCode, etPassword, etConfirmPassword;
    private Button btnGetVerificationCode, btnRegister;
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    private boolean isVerificationCodeSent = false; // 标记验证码是否已发送
    private String verifiedPhone = ""; // 记录已验证的手机号
    private static final long VERIFICATION_CODE_TIMEOUT = 60000; // 验证码过期时间，默认60秒

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        setListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tilPhone = findViewById(R.id.tilPhone);
        tilVerificationCode = findViewById(R.id.tilVerificationCode);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etPhone = findViewById(R.id.etPhone);
        etVerificationCode = findViewById(R.id.etVerificationCode);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnGetVerificationCode = findViewById(R.id.btnGetVerificationCode);
        btnRegister = findViewById(R.id.btnRegister);

        // 设置密码可见性切换图标点击事件
        tilPassword.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility();
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
        // 监听验证码输入框的焦点变化，当获得焦点时检查验证码是否过期
        etVerificationCode.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && !isVerificationCodeSent && !TextUtils.isEmpty(etVerificationCode.getText())) {
                    ToastHelper.showInfo(RegisterActivity.this, "请先获取验证码");
                    etVerificationCode.setText(""); // 清空过期的验证码
                }
            }
        });

        // 监听验证码输入框的文本变化，当用户输入验证码时检查验证码是否已过期
        etVerificationCode.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (!isVerificationCodeSent && s.length() > 0) {
                    ToastHelper.showInfo(RegisterActivity.this, "请先获取验证码");
                    etVerificationCode.setText(""); // 清空过期的验证码
                }
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnGetVerificationCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validatePhone()) {
                    String phone = etPhone.getText().toString().trim();

                    // 使用NetworkService发送验证码请求
                    android.util.Log.d("RegisterActivity", "发送验证码请求: 手机号=" + phone);

                    // 创建NetworkService实例
                    com.example.guangxiaoqing.api.NetworkService networkService =
                            new com.example.guangxiaoqing.api.NetworkService(RegisterActivity.this);

                    // 调用sendSms方法
                    networkService.sendSms(phone, "registration", new com.example.guangxiaoqing.api.NetworkCallback<Object>() {
                        @Override
                        public void onSuccess(Object response) {
                            android.util.Log.d("RegisterActivity", "验证码请求成功: " + response);
                            startCountDownTimer();
                            // 设置验证码已发送标志和已验证的手机号
                            isVerificationCodeSent = true;
                            verifiedPhone = etPhone.getText().toString().trim();
                            ToastHelper.showSuccess(RegisterActivity.this, "验证码已发送");
                        }

                        @Override
                        public void onError(String errorMessage) {
                            android.util.Log.e("RegisterActivity", "验证码请求失败: " + errorMessage);
                            // 重置验证码发送状态
                            isVerificationCodeSent = false;
                            verifiedPhone = "";
                            ToastHelper.showError(RegisterActivity.this, errorMessage);
                        }
                    });
                }
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateAllInputs()) {
                    String phone = etPhone.getText().toString().trim();
                    String verificationCode = etVerificationCode.getText().toString().trim();
                    String password = etPassword.getText().toString().trim();

                    // 使用NetworkService进行注册
                    android.util.Log.d("RegisterActivity", "发送注册请求: 手机号=" + phone + ", 验证码=" + verificationCode);

                    // 创建NetworkService实例
                    com.example.guangxiaoqing.api.NetworkService networkService =
                            new com.example.guangxiaoqing.api.NetworkService(RegisterActivity.this);

                    // 直接调用register方法，包含验证码
                    // 后端会在注册过程中验证验证码
                    networkService.register(phone, password, verificationCode, new com.example.guangxiaoqing.api.NetworkCallback<com.example.guangxiaoqing.model.TokenResponse>() {
                        @Override
                        public void onSuccess(com.example.guangxiaoqing.model.TokenResponse response) {
                            android.util.Log.d("RegisterActivity", "注册成功: token=" + response.getAccessToken());
                            ToastHelper.showSuccess(RegisterActivity.this, "注册成功");

                            // 添加延迟，确保用户能看到成功提示
                            new android.os.Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    finish();
                                }
                            }, 1500); // 延迟1.5秒后跳转
                        }

                        @Override
                        public void onError(String errorMessage) {
                            android.util.Log.e("RegisterActivity", "注册失败: " + errorMessage);
                            ToastHelper.showError(RegisterActivity.this, errorMessage);
                        }
                    });
                }
            }
        });
    }

    private boolean validatePhone() {
        String phone = etPhone.getText().toString().trim();
        if (TextUtils.isEmpty(phone)) {
            ToastHelper.showInfo(this, "请输入手机号");
            return false;
        } else if (phone.length() != 11) {
            ToastHelper.showInfo(this, "请输入11位手机号");
            return false;
        } else {
            return true;
        }
    }

    private boolean validateAllInputs() {
        boolean isValid = true;

        // 获取所有输入内容
        String phone = etPhone.getText().toString().trim();
        String verificationCode = etVerificationCode.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // 检查是否所有输入框都为空
        if (TextUtils.isEmpty(phone) && TextUtils.isEmpty(verificationCode) &&
            TextUtils.isEmpty(password) && TextUtils.isEmpty(confirmPassword)) {
            ToastHelper.showInfo(this, "请填写注册信息");
            return false;
        }

        // 验证手机号
        if (TextUtils.isEmpty(phone)) {
            ToastHelper.showInfo(this, "请输入手机号");
            isValid = false;
            return false; // 手机号为空时直接返回，不再检查其他字段
        } else if (phone.length() != 11) {
            ToastHelper.showInfo(this, "请输入11位手机号");
            isValid = false;
            return false;
        }

        // 检查验证码是否已发送
        if (!isVerificationCodeSent) {
            ToastHelper.showInfo(this, "请先获取验证码");
            return false;
        }

        // 检查手机号是否与获取验证码时的手机号一致
        if (!phone.equals(verifiedPhone)) {
            ToastHelper.showInfo(this, "手机号与获取验证码时的手机号不一致");
            return false;
        }

        // 验证验证码
        if (TextUtils.isEmpty(verificationCode)) {
            ToastHelper.showInfo(this, "请输入验证码");
            isValid = false;
            return false; // 验证码为空时直接返回
        }

        // 验证密码
        if (TextUtils.isEmpty(password)) {
            ToastHelper.showInfo(this, "请设置密码");
            isValid = false;
            return false; // 密码为空时直接返回
        } else if (password.length() < 6) {
            ToastHelper.showInfo(this, "密码长度至少为6位");
            isValid = false;
            return false;
        }

        // 验证确认密码
        if (TextUtils.isEmpty(confirmPassword)) {
            ToastHelper.showInfo(this, "请确认密码");
            isValid = false;
        } else if (!confirmPassword.equals(password)) {
            ToastHelper.showInfo(this, "两次输入的密码不一致");
            isValid = false;
        }

        return isValid;
    }

    private void startCountDownTimer() {
        if (isTimerRunning) {
            return;
        }

        isTimerRunning = true;
        btnGetVerificationCode.setEnabled(false);

        countDownTimer = new CountDownTimer(VERIFICATION_CODE_TIMEOUT, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                btnGetVerificationCode.setText(millisUntilFinished / 1000 + "秒");
            }

            @Override
            public void onFinish() {
                btnGetVerificationCode.setText("获取验证码");
                btnGetVerificationCode.setEnabled(true);
                isTimerRunning = false;

                // 验证码过期，重置验证码状态
                if (isVerificationCodeSent) {
                    isVerificationCodeSent = false;
                    verifiedPhone = "";
                    ToastHelper.showInfo(RegisterActivity.this, "验证码已过期，请重新获取");
                }
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
