package com.Dolmeng_E.workspace.domain.project.dto;

import com.Dolmeng_E.workspace.domain.project.entity.Project;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ProjectDashboardResDto {
    private BigDecimal projectMilestone;
    private Integer totalStoneCount;
    private Integer completedStoneCount;
    private Integer totalTaskCount;
    private Integer completedTaskCount;

    public static ProjectDashboardResDto fromEntity(Project project, int totalTaskCount, int completedTaskCount,
                                                    int totalStoneCount, int completedStoneCount) {
        return ProjectDashboardResDto.builder()
                .projectMilestone(project.getMilestone())
                .totalStoneCount(totalStoneCount)
                .completedStoneCount(completedStoneCount)
                .totalTaskCount(totalTaskCount)
                .completedTaskCount(completedTaskCount)
                .build();
    }
}
