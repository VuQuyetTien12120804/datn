package com.example.frontend_bookingcare.ui.chat;

public class ChatThreadMeta {
    public String threadId;
    public String title;
    public String subtitle;
    public boolean locked;
    public long updatedAtMs;
    public String lastMessage;
    public int unreadCount;

    public ChatThreadMeta() {
    }

    public ChatThreadMeta(String threadId, String title, String subtitle, boolean locked, long updatedAtMs, String lastMessage) {
        this.threadId = threadId;
        this.title = title;
        this.subtitle = subtitle;
        this.locked = locked;
        this.updatedAtMs = updatedAtMs;
        this.lastMessage = lastMessage;
        this.unreadCount = 0;
    }
}

