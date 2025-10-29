package com.Dolmeng_E.workspace.common.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoneTaskResDto {
    private List<StoneInfo> stones;
    private List<TaskInfo> tasks;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StoneInfo {
        private String stoneId;
        private String stoneName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TaskInfo {
        private String taskId;
        private String taskName;
    }
}

