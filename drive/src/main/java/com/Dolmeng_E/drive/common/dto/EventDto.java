package com.Dolmeng_E.drive.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class EventDto {
    private String rootId;
    private String rootType;
}
