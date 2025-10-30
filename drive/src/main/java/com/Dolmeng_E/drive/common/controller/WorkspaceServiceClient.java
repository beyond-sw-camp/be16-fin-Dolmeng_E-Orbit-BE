package com.Dolmeng_E.drive.common.controller;

import com.Dolmeng_E.drive.common.dto.StoneTaskResDto;
import com.Dolmeng_E.drive.common.dto.SubProjectResDto;
import com.Dolmeng_E.drive.common.dto.WorkspaceOrProjectManagerCheckDto;
import com.example.modulecommon.dto.CommonSuccessDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "workspace-service")
public interface WorkspaceServiceClient {
    // 워크스페이스 담당자 확인
    @GetMapping("/{workspaceId}/manager/check")
    ResponseEntity<CommonSuccessDto> checkWorkspaceManager(
            @PathVariable("workspaceId") String workspaceId,
            @RequestHeader("X-User-Id") String userId
    );

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

    // workspaceId 넘겼을 때 하위 프로젝트 Id, 프로젝트명 가져오는 api
    @GetMapping("/workspace/project/{workspaceId}/sub-project")
    List<SubProjectResDto> getSubProjectsByWorkspace(@PathVariable("workspaceId") String workspaceId);

    //projectId 넘겼을 때 하위 스톤 id, 테스크명 가져오는 api
    @GetMapping("/workspace/stone/{projectId}/sub-stone-task")
    StoneTaskResDto getSubStonesAndTasks(@PathVariable("projectId") String projectId);

    // stoneId 와 userId 넘겼을 때 웤스 관리자인지 프로젝트관리자인지 확인하는 api
    @GetMapping("/work-project/{stoneId}/manager/check")
    WorkspaceOrProjectManagerCheckDto checkWorkspaceOrProjectManager(
            @PathVariable("stoneId") String stoneId,
            @RequestHeader("X-User-Id") String userId
    );



}
