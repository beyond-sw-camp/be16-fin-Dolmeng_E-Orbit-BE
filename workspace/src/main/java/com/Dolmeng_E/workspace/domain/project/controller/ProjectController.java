package com.Dolmeng_E.workspace.domain.project.controller;

import com.Dolmeng_E.workspace.domain.project.dto.ProjectCreateDto;
import com.Dolmeng_E.workspace.domain.project.dto.ProjectModifyDto;
import com.Dolmeng_E.workspace.domain.project.service.ProjectService;
import com.example.modulecommon.dto.CommonSuccessDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/project")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

// 프로젝트 생성(To-Do: 스톤 생성도 구현해야함)
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

// 프로젝트 상세 조회(To-Do: 스톤, 태스트 생성 완료 후 프로젝트 구조 조회 API 구현 필요)

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

// 프로젝트가 프로젝트 캘린더에 노출 여부 설정(프로젝트 캘린더 조회용 API)

// 내 프로젝트 목록 조회

// 프로젝트 참여자 목록 조회

// 스톤 목록? 프로젝트 내에 스톤들 뿌리처럼 보이는 거


}
