package com.Dolmeng_E.workspace.domain.user_group.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class MyUserGroupDto {
    private String userGroupId;
    private String userGroupName;
}
