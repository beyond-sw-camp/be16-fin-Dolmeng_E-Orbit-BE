package com.Dolmeng_E.workspace.domain.stone.dto;

import com.Dolmeng_E.workspace.domain.stone.entity.Stone;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TopStoneCreateDto {
    private String workspaceId;
    private String participantId;
    private String projectId;
    private String stoneName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean chatCreation;
    private Boolean taskCreation;
    private BigDecimal milestone;

}
