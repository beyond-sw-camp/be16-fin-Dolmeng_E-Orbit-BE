package com.Dolmeng_E.workspace.domain.stone.dto;

import com.Dolmeng_E.workspace.domain.stone.entity.Stone;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class MilestoneResDto {
    private String stoneId;
    private LocalDateTime endTime;
    private BigDecimal milestone;

    public static MilestoneResDto fromEntity(Stone stone) {
        return MilestoneResDto.builder()
                .stoneId(stone.getId())
                .endTime(stone.getEndTime())
                .milestone(stone.getMilestone())
                .build();
    }
}
