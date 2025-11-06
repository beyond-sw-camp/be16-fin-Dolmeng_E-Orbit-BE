package com.Dolmeng_E.workspace.domain.workspace.dto;

import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceTemplates;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceDetailResDto {
    private String workspaceId;
    private String workspaceName;
    private WorkspaceTemplates workspaceTemplates;
    private LocalDateTime createdAt;
    private Integer subscribe;
    private Long currentStorage;
    private Long maxStorage;
    private Long memberCount;

    // To-Do: 프로젝트,스톤 구현 시 활용 예정
    private Long projectCount;
    private Long storageCount;
    private Long taskCount;
}
