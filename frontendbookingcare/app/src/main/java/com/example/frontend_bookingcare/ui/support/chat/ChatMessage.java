package com.example.frontend_bookingcare.ui.support.chat;

public class ChatMessage {
    public final boolean fromMe;
    public final String text;
    public final long createdAtMs;

    public ChatMessage(boolean fromMe, String text, long createdAtMs) {
        this.fromMe = fromMe;
        this.text = text != null ? text : "";
        this.createdAtMs = createdAtMs;
    }
}

