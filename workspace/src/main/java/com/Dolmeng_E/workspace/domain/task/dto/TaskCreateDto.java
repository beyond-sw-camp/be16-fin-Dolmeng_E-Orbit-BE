package com.Dolmeng_E.workspace.domain.task.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskCreateDto {
    private String stoneId; // 스톤 id
    private String managerId; // 스톤 담당자 id
    private String taskName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
