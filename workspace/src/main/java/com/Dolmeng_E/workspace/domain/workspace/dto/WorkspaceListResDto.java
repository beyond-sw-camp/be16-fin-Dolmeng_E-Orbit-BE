package com.Dolmeng_E.workspace.domain.workspace.dto;

import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceTemplates;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class WorkspaceListResDto {
    private String workspaceId;              // ws_ 접두사 ID
    private String workspaceName;            // 워크스페이스 이름
    private WorkspaceTemplates workspaceTemplates; // 템플릿 유형 (PERSONAL / PRO / ENTERPRISE)
    private Integer subscribe;               // 구독 개월 수 (추후 결제연동)
    private Long currentStorage;             // 현재 사용량
    private Long maxStorage;                 // 최대 용량
    private String role;                     // 워크스페이스 내 역할 (ADMIN / COMMON)
}
