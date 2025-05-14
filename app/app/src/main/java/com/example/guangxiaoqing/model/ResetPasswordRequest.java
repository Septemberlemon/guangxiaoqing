package com.example.guangxiaoqing.model;

/**
 * 密码重置请求模型，对应后端的ResetPasswordRequest
 */
public class ResetPasswordRequest {
    private String phone;
    private String code;
    private String new_password;

    public ResetPasswordRequest(String phone, String code, String newPassword) {
        this.phone = phone;
        this.code = code;
        this.new_password = newPassword;
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

    public String getNewPassword() {
        return new_password;
    }

    public void setNewPassword(String newPassword) {
        this.new_password = newPassword;
    }
}