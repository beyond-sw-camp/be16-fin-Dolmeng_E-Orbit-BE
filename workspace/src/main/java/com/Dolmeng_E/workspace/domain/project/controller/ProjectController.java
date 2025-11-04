package com.Dolmeng_E.workspace.domain.project.controller;

import com.Dolmeng_E.workspace.domain.project.dto.*;
import com.Dolmeng_E.workspace.domain.project.service.ProjectService;
import com.Dolmeng_E.workspace.domain.stone.dto.ProjectPeopleOverviewResDto;
import com.Dolmeng_E.workspace.domain.stone.dto.StoneSettingDto;
import com.example.modulecommon.dto.CommonSuccessDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/project")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

// 프로젝트 생성
    @PostMapping("")
    public ResponseEntity<?> createProject(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody ProjectCreateDto dto
    ) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("프로젝트 생성 완료")
                .result(projectService.createProject(userId, dto))
                .statusCode(HttpStatus.CREATED.value())
                .build()
                ,HttpStatus.CREATED);
    }

// 프로젝트 수정
    @PatchMapping("")
    public ResponseEntity<?> modifyProject(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody ProjectModifyDto dto
    ) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("프로젝트 수정 완료")
                .result(projectService.modifyProject(userId, dto))
                .statusCode(HttpStatus.OK.value())
                .build()
                ,HttpStatus.OK);
    }

// 프로젝트 목록 조회(사이드 바 프로젝트 목록)
    @GetMapping("/{workspaceId}")
    public ResponseEntity<?> getProjectList(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String workspaceId
    ) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("프로젝트 목록입니다.")
                .result(projectService.getProjectList(userId,workspaceId))
                .statusCode(HttpStatus.OK.value())
                .build()
                ,HttpStatus.OK);
    }

// 프로젝트 삭제
    @DeleteMapping("/{projectId}")
    public ResponseEntity<?> deleteProject(@RequestHeader("X-User-Id") String userId,
                                           @PathVariable String projectId
    ) {
        projectService.deleteProject(userId, projectId);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("프로젝트 삭제 완료")
                .result(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .build()
                ,HttpStatus.OK);
    }


    // 프로젝트 캘린더에 스톤 노출 여부 설정(프로젝트 캘린더 조회용 API)
    @PatchMapping("/setting")
    public ResponseEntity<?> settingProject(@RequestHeader("X-User-Id") String userId,
                                          @RequestBody ProjectSettingDto dto) {
        projectService.settingProject(userId, dto);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("프로젝트 캘린더에 스톤 노출여부 설정 완료")
                .result(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .build()
                ,HttpStatus.OK);
    }

// 내 프로젝트 목록 조회

// 프로젝트 참여자 목록 조회

// 스톤 목록 조회(root 구조)
    @GetMapping("/stones/{projectId}")
    public ResponseEntity<?> getStoneList(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String projectId
    ) {
        List<StoneListResDto> dto = projectService.getStoneList(userId,projectId);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("스톤목록 조회 완료")
                .result(dto)
                .statusCode(HttpStatus.OK.value())
                .build()
                ,HttpStatus.OK);
    }

//    프로젝트 상세조회
    @GetMapping("detail/{projectId}")
    public ResponseEntity<?> getProjectDetail(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String projectId
    ) {
        ProjectDetailResDto dto = projectService.getProjectDetail(userId, projectId);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("프로젝트 상세정보 조회 완료")
                .result(dto)
                .statusCode(HttpStatus.OK.value())
                .build()
                ,HttpStatus.OK);
    }

    // 프로젝트 대시보드용 인원 현황 API
    @GetMapping("/{projectId}/people-overview")
    public ResponseEntity<?> getProjectPeopleOverview(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String projectId
    ) {
        ProjectPeopleOverviewResDto dto = projectService.getProjectPeopleOverview(userId, projectId);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("프로젝트 참여자 개요 조회 완료")
                .result(dto)
                .statusCode(HttpStatus.OK.value())
                .build(), HttpStatus.OK);
    }

    // 프로젝트 stone, task 수 조회 API
    @GetMapping("/dashboard/{projectId}")
    public ResponseEntity<?> getProjectDashboard(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String projectId
    ) {
        ProjectDashboardResDto dto = projectService.getProjectDashboard(userId, projectId);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("프로젝트 대시보드 조회 완료")
                .result(dto)
                .statusCode(HttpStatus.OK.value())
                .build(), HttpStatus.OK);
    }



}
