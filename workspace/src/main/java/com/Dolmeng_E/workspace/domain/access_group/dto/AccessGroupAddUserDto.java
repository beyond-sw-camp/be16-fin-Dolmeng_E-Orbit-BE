package com.Dolmeng_E.workspace.domain.access_group.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Builder
@Data
public class AccessGroupAddUserDto {
    private List<UUID> userIdList;
}
