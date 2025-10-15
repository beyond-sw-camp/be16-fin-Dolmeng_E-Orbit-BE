package com.Dolmeng_E.workspace.domain.project.dto;

import com.Dolmeng_E.workspace.domain.project.entity.Project;
import com.Dolmeng_E.workspace.domain.workspace.entity.Workspace;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectCreateDto {
    private String workspaceId;
    private String projectName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String projectObjective;
    private String projectDescription;
    private String projectManagerId; //workspace 참여자 Id : 프로젝트 담당자
    private Boolean chatCreation;
    private Boolean taskCreation;

    public Project toEntity(Workspace workspace, WorkspaceParticipant workspaceParticipant) {
        return Project.builder()
                .workspace(workspace)
                .workspaceParticipant(workspaceParticipant)
                .projectName(this.projectName)
                .startTime(this.startTime)
                .endTime(this.endTime)
                .projectObjective(this.projectObjective)
                .projectDescription(this.projectDescription)
                .build();
    }
}
