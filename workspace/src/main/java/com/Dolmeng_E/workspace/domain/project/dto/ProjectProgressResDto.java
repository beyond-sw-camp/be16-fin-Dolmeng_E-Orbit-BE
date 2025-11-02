package com.Dolmeng_E.workspace.domain.project.dto;

import com.Dolmeng_E.workspace.domain.project.entity.Project;
import com.Dolmeng_E.workspace.domain.project.entity.ProjectStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProjectProgressResDto {

    private String projectId;
    private String projectName;
    private BigDecimal milestone;
    private Integer stoneCount;
    private Integer completedCount;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private ProjectStatus projectStatus;

    public static ProjectProgressResDto fromEntity(Project project) {
        return ProjectProgressResDto.builder()
                .projectId(project.getId())
                .projectName(project.getProjectName())
                .milestone(project.getMilestone() != null ? project.getMilestone() : BigDecimal.ZERO)
                .stoneCount(project.getStoneCount() != null ? project.getStoneCount() : 0)
                .completedCount(project.getCompletedCount() != null ? project.getCompletedCount() : 0)
                .startedAt(project.getStartTime())
                .endedAt(project.getEndTime())
                .projectStatus(project.getProjectStatus())
                .build();
    }
}