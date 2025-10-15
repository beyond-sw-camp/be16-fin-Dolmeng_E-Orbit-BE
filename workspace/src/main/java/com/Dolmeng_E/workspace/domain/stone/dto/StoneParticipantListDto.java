package com.Dolmeng_E.workspace.domain.stone.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Set;

@Data
@Builder
public class StoneParticipantListDto {
    String stoneId;
    Set<String> stoneParticipantList;
}
