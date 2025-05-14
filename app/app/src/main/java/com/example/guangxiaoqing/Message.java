package com.example.guangxiaoqing;

public class Message {
    private String text;
    private String timestamp;
    private boolean isSent;

    public Message(String text, String timestamp, boolean isSent) {
        this.text = text;
        this.timestamp = timestamp;
        this.isSent = isSent;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public boolean isSent() {
        return isSent;
    }
} 