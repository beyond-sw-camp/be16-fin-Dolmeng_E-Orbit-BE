package com.Dolmeng_E.workspace.domain.stone.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Set;
import java.util.UUID; // 추가

@Data
@Builder
public class StoneParticipantListDto {
    private String stoneId;
    private Set<UUID> stoneParticipantList; // String → UUID 변경 // 추가
}
