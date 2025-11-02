package com.Dolmeng_E.workspace.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class EntityNameResDto {

    private String type;   // "workspace", "project", "stone"
    private String id;
    private String name;
}