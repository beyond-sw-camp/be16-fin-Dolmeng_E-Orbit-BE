package com.Dolmeng_E.user.common.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class WorkspaceInfoResDto {
    private String workspaceId;
    private String workspaceName;
}
