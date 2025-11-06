package com.Dolmeng_E.workspace.domain.user_group.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserGroupMemberDto {
    private UUID userId;
    private String userName;
    private String userEmail;
    private String profileImageUrl;
}
