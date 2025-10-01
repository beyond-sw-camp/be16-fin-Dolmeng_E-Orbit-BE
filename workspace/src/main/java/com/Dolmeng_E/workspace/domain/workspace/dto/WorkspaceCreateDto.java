package com.Dolmeng_E.workspace.domain.workspace.dto;

import com.Dolmeng_E.workspace.domain.workspace.entity.Workspace;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceTemplates;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class WorkspaceCreateDto {

    private WorkspaceTemplates workspaceTemplates;
    private String workspaceName;
    private UUID userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Workspace toEntity(UUID userId) {
        return Workspace.builder()
                .workspaceTemplates(this.workspaceTemplates)
                .workspaceName(this.workspaceName)
                .userId(userId)
                .build();
    }
}
