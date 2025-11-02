package com.Dolmeng_E.workspace.common.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityNameReqDto {

    private String workspaceId;
    private String projectId;
    private String stoneId;
}