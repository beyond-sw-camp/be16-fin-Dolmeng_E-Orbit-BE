package com.Dolmeng_E.workspace.domain.stone.dto;

import com.Dolmeng_E.workspace.domain.stone.entity.Stone;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class MilestoneResDto {
    private String stoneId;
    private String stoneName;
    private BigDecimal milestone;
    private LocalDateTime endTime;
    private List<MilestoneResDto> children;

    public static MilestoneResDto fromEntity(Stone stone) {
        return MilestoneResDto.builder()
                .stoneId(stone.getId())
                .stoneName(stone.getStoneName())
                .milestone(stone.getMilestone())
                .endTime(stone.getEndTime())
                .children(new ArrayList<>())
                .build();
    }
}

