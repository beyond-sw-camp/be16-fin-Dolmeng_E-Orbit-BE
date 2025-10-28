package com.Dolmeng_E.workspace.domain.task.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TaskCreateDto {
    private String stoneId; // 스톤 id
    private UUID managerId; // 스톤 담당자 id
    private String taskName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
