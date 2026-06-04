package com.bookingcare.backend_bookingcare.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** Tin nhắn chatbot trả về client (Android). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotMessageDto {
    private Integer id;
    private String sender;          // user | assistant
    private String content;
    private String intent;          // chỉ assistant mới có
    private List<String> suggestions; // chỉ assistant mới có
    private long createdAtMs;
}
