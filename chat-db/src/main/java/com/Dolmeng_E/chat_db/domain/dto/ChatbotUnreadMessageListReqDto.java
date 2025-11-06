package com.Dolmeng_E.chat_db.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatbotUnreadMessageListReqDto {
    private String workspaceId;
    private String userId;
}
