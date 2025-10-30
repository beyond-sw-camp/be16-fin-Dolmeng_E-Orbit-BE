package com.Dolmeng_E.workspace.domain.stone.dto;

import com.Dolmeng_E.workspace.domain.stone.entity.Stone;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoneListResDto {
    private String stoneId;
    private String stoneName;
    private String status;
    private String startTime;
    private String endTime;

    public static StoneListResDto fromEntity(Stone stone) {
        return StoneListResDto.builder()
                .stoneId(stone.getId())
                .stoneName(stone.getStoneName())
                .status(stone.getStatus().name())
                .startTime(stone.getStartTime().toString())
                .endTime(stone.getEndTime().toString())
                .build();
    }
}
