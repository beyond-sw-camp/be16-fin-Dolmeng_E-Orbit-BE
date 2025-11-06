package com.Dolmeng_E.workspace.domain.user_group.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserGroupCreateDto {
    private String workspaceId;    // 워크스페이스 ID
    private String userGroupName;  // 사용자 그룹명
    private List<UUID> userIdList;
}
