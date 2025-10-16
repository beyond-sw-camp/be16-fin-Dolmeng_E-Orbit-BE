package com.Dolmeng_E.workspace.domain.task.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Builder
@Data
public class TaskModifyDto {
    private String taskId; // 태스크 id
    private String taskName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
