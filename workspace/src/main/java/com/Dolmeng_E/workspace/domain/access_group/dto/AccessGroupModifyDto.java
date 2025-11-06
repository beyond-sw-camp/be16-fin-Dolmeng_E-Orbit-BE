package com.Dolmeng_E.workspace.domain.access_group.dto;

import jakarta.persistence.Column;
import lombok.Data;

import java.util.List;

@Data
public class AccessGroupModifyDto {
    private String workspaceId; // 워크스페이스 식별자
    @Column(length = 10, unique = true)
    private String accessGroupName; // 현재 그룹 이름
    private List<Boolean> accessList; // 권한 체크 리스트
    @Column(length = 10, unique = true)
    private String newAccessGroupName; // 새로운 그룹 이름
}
