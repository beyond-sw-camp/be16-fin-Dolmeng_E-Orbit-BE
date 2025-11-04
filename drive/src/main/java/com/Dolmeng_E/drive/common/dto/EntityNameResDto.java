package com.Dolmeng_E.drive.common.dto;

import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Getter
@AllArgsConstructor
@Builder
public class EntityNameResDto {

    private String type;   // "workspace", "project", "stone"
    private String id;
    private String name;
}