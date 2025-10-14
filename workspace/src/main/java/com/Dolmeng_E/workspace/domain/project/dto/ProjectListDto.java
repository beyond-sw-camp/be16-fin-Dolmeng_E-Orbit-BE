package com.Dolmeng_E.workspace.domain.project.dto;

import com.Dolmeng_E.workspace.domain.project.entity.Project;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectListDto {
    private String projectId;
    private String projectName;

    public static ProjectListDto fromEntity(Project project) {
        return ProjectListDto.builder()
                .projectId(project.getId())
                .projectName(project.getProjectName())
                .build();
    }
}
