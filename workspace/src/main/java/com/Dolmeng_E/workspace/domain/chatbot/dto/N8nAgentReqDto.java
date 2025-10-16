package com.Dolmeng_E.workspace.domain.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class N8nAgentReqDto {
    private String userId;
    private String userName;
    private String workspaceId;
    private String content;
}
