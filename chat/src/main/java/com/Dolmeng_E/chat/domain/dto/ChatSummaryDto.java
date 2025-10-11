package com.Dolmeng_E.chat.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSummaryDto {
    private Long roomId;           // 채팅방 ID
    private String lastMessage;    // 마지막 메시지 내용
    private String lastSenderEmail;     // 마지막 메시지 보낸 사람 이메일 or 이름
    private Long unreadCount;       // 현재 사용자의 안읽은 메시지 수
}

