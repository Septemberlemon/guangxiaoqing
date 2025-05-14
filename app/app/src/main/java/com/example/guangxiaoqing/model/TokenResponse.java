package com.example.guangxiaoqing.model;

/**
 * Token响应模型，对应后端的Token响应
 */
public class TokenResponse {
    private String access_token;
    private String token_type;

    public String getAccessToken() {
        return access_token;
    }

    public void setAccessToken(String accessToken) {
        this.access_token = accessToken;
    }

    public String getTokenType() {
        return token_type;
    }

    public void setTokenType(String tokenType) {
        this.token_type = tokenType;
    }

    /**
     * 获取完整的认证头信息
     * @return 格式为"Bearer {token}"的认证头
     */
    public String getAuthHeader() {
        return token_type + " " + access_token;
    }
}