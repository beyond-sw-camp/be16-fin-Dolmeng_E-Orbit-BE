package com.Dolmeng_E.user.common.service;

import com.Dolmeng_E.user.common.dto.WorkspaceInfoResDto;
import com.Dolmeng_E.user.common.dto.WorkspaceNameDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "workspace-service")
public interface WorkspaceFeign {

    // 회원 id + 워크스페이스명
    @PostMapping("/workspace/return")
    WorkspaceInfoResDto fetchWorkspaceInfo(@RequestHeader("X-User-Id")String userId, @RequestBody WorkspaceNameDto workspaceName);

    // 워크스페이스 존재 여부 검증
    @GetMapping("/workspace/{workspaceId}/exists")
    Boolean checkWorkspaceExists(@PathVariable("workspaceId") String workspaceId);

    // 유저 멤버 여부 검증
    @GetMapping("/workspace/{workspaceId}/participants/{userId}/exists")
    Boolean checkWorkspaceMembership(@PathVariable("workspaceId") String workspaceId,
                                     @PathVariable("userId") UUID userId);

}
