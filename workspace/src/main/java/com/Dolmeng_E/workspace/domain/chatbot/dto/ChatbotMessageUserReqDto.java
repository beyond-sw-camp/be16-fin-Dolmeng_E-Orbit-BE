package com.Dolmeng_E.workspace.domain.chatbot.dto;

import com.Dolmeng_E.workspace.domain.chatbot.entity.ChatbotMessageType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatbotMessageUserReqDto {
    @NotEmpty(message = "workspaceId가 비어있습니다.")
    private String workspaceId;
    @NotEmpty(message = "content가 비어있습니다.")
    private String content;
}
