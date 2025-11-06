package com.Dolmeng_E.workspace.common.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkspaceInfoResDto {
    private String workspaceId;
    private String workspaceName;
}
