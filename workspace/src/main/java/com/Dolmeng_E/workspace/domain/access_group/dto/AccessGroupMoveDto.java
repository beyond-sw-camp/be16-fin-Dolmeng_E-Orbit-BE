package com.Dolmeng_E.workspace.domain.access_group.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class AccessGroupMoveDto {
    private List<UUID> userIdList;
}
