package com.Dolmeng_E.workspace.domain.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskModifyDto {
    private String taskId; // 태스크 id
    private String taskName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private UUID NewManagerUserId;
}
