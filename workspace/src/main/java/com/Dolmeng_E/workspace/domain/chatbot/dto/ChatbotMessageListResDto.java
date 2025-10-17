package com.Dolmeng_E.workspace.domain.chatbot.dto;

import com.Dolmeng_E.workspace.domain.chatbot.entity.ChatbotMessage;
import com.Dolmeng_E.workspace.domain.chatbot.entity.ChatbotMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatbotMessageListResDto {
    private String content;
    private ChatbotMessageType type;

    public static ChatbotMessageListResDto fromEntity(ChatbotMessage chatbotMessage) {
        return ChatbotMessageListResDto.builder()
                .content(chatbotMessage.getContent())
                .type(chatbotMessage.getType())
                .build();
    }
}
