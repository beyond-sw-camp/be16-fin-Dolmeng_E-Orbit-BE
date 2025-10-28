package com.Dolmeng_E.workspace.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GetSchedulesForChatBotReqDto {
    private String workspaceId;
    private LocalDateTime endedAt;
}
