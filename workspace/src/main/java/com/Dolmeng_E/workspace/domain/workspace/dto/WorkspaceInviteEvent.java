package com.Dolmeng_E.workspace.domain.workspace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
// 트랜잭션 커밋 후 리스너로 전달되는 데이터
public class WorkspaceInviteEvent {
    private final String email;
    private final String workspaceName;
    private final String token;
}