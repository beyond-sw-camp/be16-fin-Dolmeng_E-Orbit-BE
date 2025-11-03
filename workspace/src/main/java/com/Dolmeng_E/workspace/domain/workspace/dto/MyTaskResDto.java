package com.Dolmeng_E.workspace.domain.workspace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MyTaskResDto {
    private String taskId;
    private String taskName;
    private String projectName;
    private String stoneName;
    private boolean isDone;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal stoneMilestone;
    private String stoneId;
}
