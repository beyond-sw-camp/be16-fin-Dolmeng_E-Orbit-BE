package com.Dolmeng_E.workspace.domain.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatbotInfoReqDto {
    private String workspaceId;
    private String userId;
    private String endTime;
    private String content;
    private String projectName;
}
