package com.Dolmeng_E.workspace.domain.stone.dto;

import com.Dolmeng_E.workspace.domain.stone.entity.Stone;
import com.Dolmeng_E.workspace.domain.task.entity.Task;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TaskResDto {

    private String taskId;

    private String taskName;

    private UUID taskManagerUserId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Boolean isDone;

    // 피그마에는 없음, 예비용
    private String taskManagerName;

    public static TaskResDto fromEntity(Task task) {
        return TaskResDto.builder()
                .taskId(task.getId())
                .taskName(task.getTaskName())
                .taskManagerUserId(task.getTaskManager().getUserId())
                .startTime(task.getStartTime())
                .endTime(task.getEndTime())
                .isDone(task.getIsDone())
                .taskManagerName(task.getTaskManager().getUserName())
                .build();
    }

}
