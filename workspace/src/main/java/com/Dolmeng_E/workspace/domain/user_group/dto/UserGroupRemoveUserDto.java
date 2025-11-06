package com.Dolmeng_E.workspace.domain.user_group.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class UserGroupRemoveUserDto {
    private List<UUID> userIdList;
}
