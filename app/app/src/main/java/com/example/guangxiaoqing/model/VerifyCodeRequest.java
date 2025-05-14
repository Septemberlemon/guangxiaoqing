package com.example.guangxiaoqing.model;

/**
 * 验证码验证请求模型，对应后端的VerifyCodeRequest
 */
public class VerifyCodeRequest {
    private String phone;
    private String code;
    private String type; // "registration" 或 "password_reset"

    public VerifyCodeRequest(String phone, String code, String type) {
        this.phone = phone;
        this.code = code;
        this.type = type;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}