package com.example.frontend_bookingcare.ui.messages;

public class MessageThread {
    public final String threadId;
    public final String title;
    /** Dòng phụ dưới tiêu đề (vd. chuyên khoa), không phải tên bệnh nhân. */
    public final String subtitle;
    public final String preview;
    public final String timeLabel;
    public final boolean locked;
    public final int unreadCount;
    /** Subtitle truyền sang màn chat (vd. "Bác sĩ: …"). */
    public final String chatSubtitle;

    public MessageThread(String threadId, String title, String subtitle, String preview,
                         String timeLabel, boolean locked, int unreadCount, String chatSubtitle) {
        this.threadId = threadId;
        this.title = title;
        this.subtitle = subtitle;
        this.preview = preview;
        this.timeLabel = timeLabel;
        this.locked = locked;
        this.unreadCount = unreadCount;
        this.chatSubtitle = chatSubtitle;
    }
}
