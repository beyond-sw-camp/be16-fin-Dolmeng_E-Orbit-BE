package com.Dolmeng_E.workspace.domain.user_group.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserGroupSearchRestDto {
    private String userGroupName;
    private String groupName;
    private LocalDateTime createdAt;
    private long userGroupParticipantsCount;
}
