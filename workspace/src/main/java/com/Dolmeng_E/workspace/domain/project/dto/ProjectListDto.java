package com.Dolmeng_E.workspace.domain.project.dto;

import com.Dolmeng_E.workspace.domain.project.entity.Project;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectListDto {
    private String projectId;
    private String projectName;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    public static ProjectListDto fromEntity(Project project) {
        return ProjectListDto.builder()
                .projectId(project.getId())
                .projectName(project.getProjectName())
                .startedAt(project.getStartTime())
                .endedAt(project.getEndTime())
                .build();
    }
}
