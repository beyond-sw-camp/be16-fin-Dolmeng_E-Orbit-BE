package com.Dolmeng_E.workspace.domain.user_group.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserGroupModifyDto {
    private String userGroupId;    // 유저그룹 ID
    private String userGroupName;  // 사용자 그룹명
    private List<UUID> userIdList;
}
