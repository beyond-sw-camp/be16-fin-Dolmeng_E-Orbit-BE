package com.Dolmeng_E.workspace.domain.stone.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID; // 추가

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class StoneCreateDto {
    private String parentStoneId;
    private String stoneName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean chatCreation;
    private BigDecimal milestone;
    private String stoneDescribe;
    private Set<UUID> participantIds; // String → UUID 변경 // 추가
}
