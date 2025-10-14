package com.Dolmeng_E.workspace.domain.project.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectDeleteDto {
    private String workspaceId;
    private String projectId;
}
