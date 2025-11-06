package com.Dolmeng_E.workspace.common.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatInviteReqDto {
    @NotEmpty(message = "workspace id가 비어있습니다.")
    private String workspaceId;
    @NotEmpty(message = "project id가 비어있습니다.")
    private String projectId;
    @NotEmpty(message = "stone id가 비어있습니다.")
    private String stoneId;
    @Builder.Default
    List<UUID> userIdList = new ArrayList<>();
}
