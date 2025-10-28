package com.Dolmeng_E.workspace.domain.stone.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID; // 추가

@Data
@Builder
public class StoneCreateDto {
    private String parentStoneId;
    private String stoneName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean chatCreation;
    private BigDecimal milestone;
    private Set<UUID> participantIds; // String → UUID 변경 // 추가
}
