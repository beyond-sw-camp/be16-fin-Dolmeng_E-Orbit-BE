package com.Dolmeng_E.workspace.domain.stone.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID; // 추가

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class StoneParticipantListDto {
    private String stoneId;
    private Set<UUID> stoneParticipantList; // String → UUID 변경 // 추가
}
