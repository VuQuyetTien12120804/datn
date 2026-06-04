package com.example.frontend_bookingcare.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** DTO 1 tin nhắn chatbot trả về từ backend. */
public class ChatbotMessageDto {
    public Integer id;
    public String sender;            // "user" | "assistant"
    public String content;
    public String intent;            // chỉ assistant
    public List<String> suggestions; // chỉ assistant — chip gợi ý
    @SerializedName("createdAtMs")
    public long createdAtMs;
}
