package com.Dolmeng_E.workspace.domain.stone.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StoneManagerModifyDto {
    private String stoneId;
    private UUID newManagerUserId;
}
