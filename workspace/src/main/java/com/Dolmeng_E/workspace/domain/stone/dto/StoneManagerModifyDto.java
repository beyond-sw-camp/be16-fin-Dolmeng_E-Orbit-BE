package com.Dolmeng_E.workspace.domain.stone.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class StoneManagerModifyDto {
    private String stoneId;
    private UUID newManagerUserId;
}
