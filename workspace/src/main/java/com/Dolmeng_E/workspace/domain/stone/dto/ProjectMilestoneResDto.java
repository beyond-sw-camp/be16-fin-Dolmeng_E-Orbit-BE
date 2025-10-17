package com.Dolmeng_E.workspace.domain.stone.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ProjectMilestoneResDto {
    private String projectId;
    private String projectName;
    private List<MilestoneResDto> milestoneResDtoList;
}
