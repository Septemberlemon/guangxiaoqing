package com.example.guangxiaoqing.model;

/**
 * 聊天响应模型，对应后端的ChatResponse
 */
public class ChatResponse {
    private String content;

    public ChatResponse() {
    }

    public ChatResponse(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
