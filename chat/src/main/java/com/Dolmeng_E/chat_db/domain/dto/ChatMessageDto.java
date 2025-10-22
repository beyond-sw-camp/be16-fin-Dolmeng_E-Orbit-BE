package com.Dolmeng_E.chat_db.domain.dto;

import com.Dolmeng_E.chat_db.domain.entity.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {
    private Long roomId;
    private String message;
    private String senderId;
    private String senderName;
    private LocalDateTime lastSendTime;
    private String userProfileImageUrl;
    private MessageType messageType;
    private List<ChatFileListDto> chatFileListDtoList = new ArrayList<>();
}
