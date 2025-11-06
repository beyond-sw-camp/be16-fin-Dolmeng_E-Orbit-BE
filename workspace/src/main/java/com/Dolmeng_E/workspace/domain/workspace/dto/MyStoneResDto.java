package com.Dolmeng_E.workspace.domain.workspace.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyStoneResDto {
    private String stoneId;
    private String stoneName;
    private String projectName;
    private BigDecimal milestone;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String projectId;
}
