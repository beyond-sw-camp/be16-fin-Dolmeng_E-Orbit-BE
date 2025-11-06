package com.Dolmeng_E.drive.common.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityNameReqDto {

    private String workspaceId;
    private String projectId;
    private String stoneId;
}