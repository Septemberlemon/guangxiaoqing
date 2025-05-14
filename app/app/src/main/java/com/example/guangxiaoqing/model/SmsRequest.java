package com.example.guangxiaoqing.model;

/**
 * 短信验证码请求模型，对应后端的SendSMSRequest
 */
public class SmsRequest {
    private String phone;
    private String type; // "registration" 或 "password_reset"

    public SmsRequest(String phone, String type) {
        this.phone = phone;
        this.type = type;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}