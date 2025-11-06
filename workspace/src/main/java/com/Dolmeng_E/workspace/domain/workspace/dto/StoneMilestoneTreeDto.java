package com.Dolmeng_E.workspace.domain.workspace.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class StoneMilestoneTreeDto {
    private String stoneId;
    private String parentStoneId;
    private String stoneName;
    private BigDecimal milestone; // ex. 35.5%
}