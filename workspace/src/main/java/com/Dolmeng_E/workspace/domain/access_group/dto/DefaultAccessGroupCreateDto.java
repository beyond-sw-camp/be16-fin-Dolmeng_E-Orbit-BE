package com.Dolmeng_E.workspace.domain.access_group.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class DefaultAccessGroupCreateDto {
    private String workspaceId; // 워크스페이스 식별자
}
