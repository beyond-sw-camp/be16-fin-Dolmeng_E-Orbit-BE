package com.Dolmeng_E.workspace.domain.project.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class StoneListResDto {
    private String projectId;
    private String stoneId;
    private String stoneName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String parentStoneId;
    private List<StoneListResDto> childStone;
    private BigDecimal milestone;
    private LocalDateTime createdAt;
}