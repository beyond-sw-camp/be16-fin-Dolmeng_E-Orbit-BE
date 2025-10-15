package com.Dolmeng_E.workspace.domain.stone.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StoneManagerModifyDto {
    private String stoneId;
    private String newManagerId;
}
