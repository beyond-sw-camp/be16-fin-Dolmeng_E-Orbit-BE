package com.Dolmeng_E.workspace.domain.workspace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriveKafkaReqDto {
    private String rootId;
    private String rootType;
}
