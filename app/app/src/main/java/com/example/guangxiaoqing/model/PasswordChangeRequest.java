package com.example.guangxiaoqing.model;

/**
 * 密码修改请求模型，对应后端的PasswordChange
 */
public class PasswordChangeRequest {
    private String phone;
    private String old_password;
    private String new_password;

    public PasswordChangeRequest(String phone, String oldPassword, String newPassword) {
        this.phone = phone;
        this.old_password = oldPassword;
        this.new_password = newPassword;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getOldPassword() {
        return old_password;
    }

    public void setOldPassword(String oldPassword) {
        this.old_password = oldPassword;
    }

    public String getNewPassword() {
        return new_password;
    }

    public void setNewPassword(String newPassword) {
        this.new_password = newPassword;
    }
}