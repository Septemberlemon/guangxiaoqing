package com.example.guangxiaoqing.model;

/**
 * 注册请求模型，对应后端的UserCreate
 */
public class RegisterRequest {
    private String phone;
    private String password;
    private String code; // 验证码

    public RegisterRequest(String phone, String password, String code) {
        this.phone = phone;
        this.password = password;
        this.code = code;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}