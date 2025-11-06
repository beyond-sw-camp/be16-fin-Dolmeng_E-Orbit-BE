package com.Dolmeng_E.workspace.domain.access_group.dto;

import jakarta.persistence.Column;
import lombok.Data;
import java.util.List;

@Data
public class CustomAccessGroupDto {
    private String workspaceId; // 워크스페이스 식별자
    @Column(length = 10, unique = true)
    private String accessGroupName;
    private List<Boolean> accessList; // 권한 체크 리스트
}
