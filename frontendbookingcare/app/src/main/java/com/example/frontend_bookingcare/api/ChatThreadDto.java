package com.example.frontend_bookingcare.api;

import java.util.List;

public class ChatThreadDto {
    public Integer threadId;
    public String threadKey;
    public String threadType;
    public String title;
    public String subtitle;
    public String lastMessage;
    public long updatedAtMs;
    public int unreadCount;
    public boolean locked;
    public boolean canSend;
}
