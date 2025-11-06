package com.Dolmeng_E.workspace.domain.user_group.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserGroupListReqDto {
    private String workspaceId;
}
