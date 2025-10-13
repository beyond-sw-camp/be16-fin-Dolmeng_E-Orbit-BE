package com.Dolmeng_E.workspace.domain.workspace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceParticipantResDto {
    private UUID userId;
    private String userName;
    private String workspaceRole;
    private String accessGroupId;
    private String accessGroupName;
    private boolean isDeleted;
}
