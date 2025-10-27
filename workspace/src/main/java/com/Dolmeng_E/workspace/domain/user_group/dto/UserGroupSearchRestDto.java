package com.Dolmeng_E.workspace.domain.user_group.dto;

import com.Dolmeng_E.workspace.common.dto.UserInfoResDto;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class UserGroupSearchRestDto {
    private String userGroupName;
    private String groupName;
    private LocalDateTime createdAt;
    private long userGroupParticipantsCount;
    private List<UserInfoResDto> participants;
}
