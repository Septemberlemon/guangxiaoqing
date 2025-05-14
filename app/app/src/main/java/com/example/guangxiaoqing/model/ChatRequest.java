package com.example.guangxiaoqing.model;

import java.util.List;

/**
 * 聊天请求模型，对应后端的chat接口
 */
public class ChatRequest {
    private String content;
    private List<ChatMessage> history;

    public ChatRequest(String content) {
        this.content = content;
    }
    
    public ChatRequest(String content, List<ChatMessage> history) {
        this.content = content;
        this.history = history;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
    
    public List<ChatMessage> getHistory() {
        return history;
    }
    
    public void setHistory(List<ChatMessage> history) {
        this.history = history;
    }
}