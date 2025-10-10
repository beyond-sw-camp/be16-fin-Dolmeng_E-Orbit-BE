package com.Dolmeng_E.chat.domain.dto;

import com.Dolmeng_E.chat.domain.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {
//    private Long roomId;
    private String message;
    private String senderEmail;

    public static ChatMessageDto fromEntity(ChatMessage chatMessage, String email) {
        return ChatMessageDto.builder()
                .message(chatMessage.getContent())
                .senderEmail(email)
                .build();
    }
}
