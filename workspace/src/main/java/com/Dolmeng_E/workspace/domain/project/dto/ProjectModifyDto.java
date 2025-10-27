package com.Dolmeng_E.workspace.domain.project.dto;

import com.Dolmeng_E.workspace.domain.project.entity.Project;
import com.Dolmeng_E.workspace.domain.project.entity.ProjectStatus;
import com.Dolmeng_E.workspace.domain.workspace.entity.Workspace;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ProjectModifyDto {
    private String workspaceId;
    private String projectId;
    private String projectName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String projectObjective;
    private String projectDescription;
    private ProjectStatus projectStatus;
    private UUID projectManagerId; // 새 담당자

    public Project toEntity(Workspace workspace, WorkspaceParticipant workspaceParticipant) {
        return Project.builder()
                .workspace(workspace)
                .workspaceParticipant(workspaceParticipant)
                .projectName(this.projectName)
                .startTime(this.startTime)
                .endTime(this.endTime)
                .projectObjective(this.projectObjective)
                .projectDescription(this.projectDescription)
                .projectStatus(this.projectStatus)
                .build();
    }
}
