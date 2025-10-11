package com.Dolmeng_E.workspace.domain.access_group.dto;

import com.Dolmeng_E.workspace.common.dto.UserInfoResDto;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccessGroupUserDetailDto {
    private UserInfoResDto userInfo;   // 사용자 정보
    private WorkspaceRole workspaceRole; // ADMIN / COMMON
}