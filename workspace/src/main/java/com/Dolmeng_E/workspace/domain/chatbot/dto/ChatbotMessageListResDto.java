package com.Dolmeng_E.workspace.domain.chatbot.dto;

import com.Dolmeng_E.workspace.domain.chatbot.entity.ChatbotMessage;
import com.Dolmeng_E.workspace.domain.chatbot.entity.ChatbotMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatbotMessageListResDto {
    private String content;
    private ChatbotMessageType type;
    private LocalDateTime timestamp;

    public static ChatbotMessageListResDto fromEntity(ChatbotMessage chatbotMessage) {
        return ChatbotMessageListResDto.builder()
                .content(chatbotMessage.getContent())
                .type(chatbotMessage.getType())
                .timestamp(chatbotMessage.getCreatedAt())
                .build();
    }
}
