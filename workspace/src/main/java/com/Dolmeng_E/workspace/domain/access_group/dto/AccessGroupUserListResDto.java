package com.Dolmeng_E.workspace.domain.access_group.dto;

import com.Dolmeng_E.workspace.common.dto.UserInfoResDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccessGroupUserListResDto {
    private String groupId;
    private String groupName;
    private List<AccessGroupUserDetailDto> userList;
}