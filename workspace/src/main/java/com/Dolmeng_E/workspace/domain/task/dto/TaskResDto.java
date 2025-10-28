package com.Dolmeng_E.workspace.domain.task.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TaskResDto {
    private String taskId;

    private String taskManagerId;

    private UUID taskManagerUserId;

    private String taskName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Boolean isDone;
}
