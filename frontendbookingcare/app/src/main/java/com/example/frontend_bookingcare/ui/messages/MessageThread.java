package com.example.frontend_bookingcare.ui.messages;

public class MessageThread {
    public final String threadId;
    public final String title;
    public final String patientName;
    public final String preview;
    public final String timeLabel;
    public final boolean locked;
    public final int unreadCount;

    public MessageThread(String threadId, String title, String patientName, String preview, String timeLabel, boolean locked, int unreadCount) {
        this.threadId = threadId;
        this.title = title;
        this.patientName = patientName;
        this.preview = preview;
        this.timeLabel = timeLabel;
        this.locked = locked;
        this.unreadCount = unreadCount;
    }
}

