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
public class PersonalWorkspaceCreateDto {

    private WorkspaceTemplates workspaceTemplates; // PERSONAL
    private String workspaceName; // 워크스페이스 명
    private UUID userId;
    private String userName;

    public Workspace toEntity() {
        return Workspace.builder()
                .workspaceTemplates(this.workspaceTemplates)
                .workspaceName(this.workspaceName)
                .userId(this.userId)
                .build();
    }

}
