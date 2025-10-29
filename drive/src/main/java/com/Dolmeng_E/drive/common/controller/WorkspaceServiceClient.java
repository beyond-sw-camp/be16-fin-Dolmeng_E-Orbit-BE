package com.Dolmeng_E.drive.common.controller;

import com.example.modulecommon.dto.CommonSuccessDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "workspace-service")
public interface WorkspaceServiceClient {
    // 워크스페이스 참여자 확인
    @GetMapping("/workspace/{workspaceId}/members/check")
    ResponseEntity<CommonSuccessDto> checkWorkspaceMembership(
            @PathVariable("workspaceId") String workspaceId,
            @RequestHeader("X-User-Id") String userId
    );

    // 프로젝트 담당자 확인
    @GetMapping("/workspace/project/{projectId}/manager/check")
    ResponseEntity<CommonSuccessDto> checkProjectManagership(
            @PathVariable("projectId") String projectId,
            @RequestHeader("X-User-Id") String userId
    );

    // 프로젝트 참여자 확인
    @GetMapping("/workspace/project/{projectId}/members/check")
    ResponseEntity<CommonSuccessDto> checkProjectMembership(
            @PathVariable("projectId") String projectId,
            @RequestHeader("X-User-Id") String userId
    );

    // 스톤 담당자 확인
    @GetMapping("/workspace/stone/{stoneId}/manager/check")
    ResponseEntity<CommonSuccessDto> checkStoneManagership(
            @PathVariable("stoneId") String stoneId,
            @RequestHeader("X-User-Id") String userId
    );

    // 스톤 참여자 확인
    @GetMapping("/workspace/stone/{stoneId}/members/check")
    ResponseEntity<CommonSuccessDto> checkStoneMembership(
            @PathVariable("stoneId") String stoneId,
            @RequestHeader("X-User-Id") String userId
    );

}
