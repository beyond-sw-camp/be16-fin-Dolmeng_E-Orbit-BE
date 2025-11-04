package com.Dolmeng_E.workspace.domain.stone.dto;

import com.Dolmeng_E.workspace.domain.task.entity.Task;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubTaskResDto {
    private String taskId;
    private String taskName;

    public SubTaskResDto(Task task) {
        this.taskId = task.getId();
        this.taskName = task.getTaskName();
    }
}
