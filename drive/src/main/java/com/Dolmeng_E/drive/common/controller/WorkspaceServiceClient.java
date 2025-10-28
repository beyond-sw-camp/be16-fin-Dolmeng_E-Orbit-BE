package com.Dolmeng_E.drive.common.controller;

import com.example.modulecommon.dto.CommonSuccessDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "workspace-service")
public interface WorkspaceServiceClient {
    @GetMapping("/workspace/{workspaceId}/members/check")
    ResponseEntity<CommonSuccessDto> checkWorkspaceMembership(
            @PathVariable("workspaceId") String workspaceId,
            @RequestHeader("X-User-Id") String userId
    );

    @GetMapping("/project/{projectId}/members/check")
    ResponseEntity<CommonSuccessDto> checkProjectMembership(
            @PathVariable("projectId") String workspaceId,
            @RequestHeader("X-User-Id") String userId
    );

    @GetMapping("/stone/{stoneId}/members/check")
    ResponseEntity<CommonSuccessDto> checkStoneMembership(
            @PathVariable("stoneId") String workspaceId,
            @RequestHeader("X-User-Id") String userId
    );
}
