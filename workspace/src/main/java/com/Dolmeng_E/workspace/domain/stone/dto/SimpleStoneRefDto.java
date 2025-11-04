package com.Dolmeng_E.workspace.domain.stone.dto;

import jakarta.annotation.security.DenyAll;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class SimpleStoneRefDto {
    private String stoneId;
    private String stoneName;
}
