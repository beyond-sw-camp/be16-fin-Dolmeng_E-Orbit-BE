package com.Dolmeng_E.workspace.domain.project.dto;

import com.Dolmeng_E.workspace.domain.project.entity.Project;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProjectProgressResDto {

    private String projectId;
    private String projectName;
    private BigDecimal milestone;
    private Integer stoneCount;
    private Integer completedCount;

    public static ProjectProgressResDto fromEntity(Project project) {
        return ProjectProgressResDto.builder()
                .projectId(project.getId())
                .projectName(project.getProjectName())
                .milestone(project.getMilestone() != null ? project.getMilestone() : BigDecimal.ZERO)
                .stoneCount(project.getStoneCount() != null ? project.getStoneCount() : 0)
                .completedCount(project.getCompletedCount() != null ? project.getCompletedCount() : 0)
                .build();
    }
}