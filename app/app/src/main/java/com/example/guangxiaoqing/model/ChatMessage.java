package com.example.guangxiaoqing.model;

/**
 * 聊天消息模型，用于发送历史消息上下文
 */
public class ChatMessage {
    private String role; // "user" 或 "assistant"
    private String content;

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
} 