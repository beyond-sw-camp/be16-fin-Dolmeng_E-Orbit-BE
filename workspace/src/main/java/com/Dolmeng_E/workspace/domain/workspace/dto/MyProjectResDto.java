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
public class MyProjectResDto {
    private String projectId;
    private String projectName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal milestone;  // 진행률 (%)
}
