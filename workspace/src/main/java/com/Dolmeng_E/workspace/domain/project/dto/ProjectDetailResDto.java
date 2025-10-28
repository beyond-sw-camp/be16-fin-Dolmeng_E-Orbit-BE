package com.Dolmeng_E.workspace.domain.project.dto;

import com.Dolmeng_E.workspace.domain.project.entity.ProjectStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProjectDetailResDto {
    private String projectId;

    private String projectName;

    // 프로젝트 목표
    private String projectObjective;

    // 프로젝트 설명
    private String projectDescription;

    // 프로젝트 마일스톤
    private BigDecimal milestone;

    // 프로젝트 시작기간
    private LocalDateTime startTime;

    // 프로젝트 종료기간
    private LocalDateTime endTime;

    private ProjectStatus projectStatus; // PROGRESS,COMPLETED,STORAGE

    private Boolean isDelete;

    // 전체 스톤 수
    private Integer stoneCount;

    // 완료된 스톤 수
    private Integer completedCount;

    private String projectManagerName;
}
