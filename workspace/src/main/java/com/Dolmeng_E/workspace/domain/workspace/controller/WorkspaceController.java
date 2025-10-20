package com.Dolmeng_E.workspace.domain.workspace.controller;

import com.Dolmeng_E.workspace.common.dto.WorkspaceInfoResDto;
import com.Dolmeng_E.workspace.common.dto.WorkspaceNameDto;
import com.Dolmeng_E.workspace.domain.project.dto.ProjectProgressResDto;
import com.Dolmeng_E.workspace.domain.project.service.ProjectService;
import com.Dolmeng_E.workspace.domain.workspace.dto.*;
import com.Dolmeng_E.workspace.domain.workspace.repository.WorkspaceParticipantRepository;
import com.Dolmeng_E.workspace.domain.workspace.service.WorkspaceService;
import com.example.modulecommon.dto.CommonSuccessDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/workspace")
@RequiredArgsConstructor
public class WorkspaceController {
    private final WorkspaceService workspaceService;

//    새 워크스페이스 생성
    @PostMapping("")
    public ResponseEntity<?> createWorkspace(@RequestBody WorkspaceCreateDto workspaceCreateDto, @RequestHeader("X-User-Id") String userId) {
        String workspaceId = workspaceService.createWorkspace(workspaceCreateDto, userId);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(workspaceId)
                .statusCode(HttpStatus.CREATED.value())
                .statusMessage("워크스페이스 생성 성공")
                .build(),
                HttpStatus.CREATED);
    }

//    회원가입 시 개인 워크스페이스 생성


//    워크스페이스 목록 조회
    @GetMapping("")
    public ResponseEntity<?> getWorkspaceList(@RequestHeader("X-User-Id") String userId) {

        List<WorkspaceListResDto> workspaces = workspaceService.getWorkspaceList(userId);

        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusCode(HttpStatus.OK.value())
                .statusMessage("워크스페이스 목록 조회 성공")
                .result(workspaces)
                .build(),
                HttpStatus.OK);
    }
//    워크스페이스 상세조회
    @GetMapping("/{workspaceId}")
        public ResponseEntity<?> getWorkspaceDetail(
                @RequestHeader("X-User-Id") String userId,
                @PathVariable String workspaceId
        ) {
            WorkspaceDetailResDto workspaceDetail = workspaceService.getWorkspaceDetail(userId, workspaceId);

            return new ResponseEntity<>(CommonSuccessDto.builder()
                    .statusCode(HttpStatus.OK.value())
                    .statusMessage("워크스페이스 상세 조회 성공")
                    .result(workspaceDetail)
                    .build(),
                    HttpStatus.OK);
        }

//    워크스페이스 변경(To-do: 관리자 그룹, 일반사용자 그룹은 이름 바꾸지 못하게)
    @PatchMapping("/{workspaceId}/name")
    public ResponseEntity<?> updateWorkspaceName(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String workspaceId,
            @RequestBody WorkspaceNameUpdateDto dto
    ) {
        workspaceService.updateWorkspaceName(userId, workspaceId, dto);

        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusCode(HttpStatus.OK.value())
                .statusMessage("워크스페이스명 변경 성공")
                .result("변경된 이름: " + dto.getWorkspaceName())
                .build(),
                HttpStatus.OK);
    }

//    워크스페이스 회원 초대
    @PostMapping("/{workspaceId}/participants")
    public ResponseEntity<CommonSuccessDto> addWorkspaceParticipants(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String workspaceId,
            @RequestBody WorkspaceAddUserDto dto
    ) {
        workspaceService.addParticipants(userId, workspaceId, dto);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result("워크스페이스 사용자 추가 완료")
                .statusCode(HttpStatus.OK.value())
                .statusMessage("워크스페이스 사용자 추가 성공")
                .build(),
                HttpStatus.OK);
    }


//    To-Do: throw로 던졌지만 공통 에러 코드로 잡아야함
//    워크스페이스 이메일 회원 초대 (메일 발송)
    @PostMapping("/{workspaceId}/invite")
    public ResponseEntity<CommonSuccessDto> inviteUsersToWorkspace(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String workspaceId,
            @RequestBody WorkspaceInviteDto dto
    ) throws AccessDeniedException {
        workspaceService.inviteUsers(userId, workspaceId, dto);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result("초대 메일 발송 완료")
                .statusCode(HttpStatus.OK.value())
                .statusMessage("워크스페이스 이메일 초대 성공")
                .build(),
                HttpStatus.OK);
    }

//    워크스페이스 참여자 목록 조회
    @GetMapping("/{workspaceId}/participants")
    public ResponseEntity<?> getWorkspaceParticipants(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String workspaceId
    ) {
        List<WorkspaceParticipantResDto> participants = workspaceService.getWorkspaceParticipants(userId, workspaceId);

        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusCode(HttpStatus.OK.value())
                .statusMessage("워크스페이스 참여자 목록 조회 성공")
                .result(participants)
                .build(),
                HttpStatus.OK);
    }

//    워크스페이스 회원 삭제
    @DeleteMapping("/{workspaceId}/participants")
    public ResponseEntity<?> deleteWorkspaceParticipants(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String workspaceId,
            @RequestBody WorkspaceDeleteUserDto dto
    ) {
        workspaceService.deleteWorkspaceParticipants(userId, workspaceId, dto);

        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusCode(HttpStatus.OK.value())
                .statusMessage("워크스페이스 회원 삭제 성공")
                .result(dto.getUserIdList())
                .build(),
                HttpStatus.OK);
    }

    @DeleteMapping("/{workspaceId}")
    public ResponseEntity<?> deleteWorkspace(@RequestHeader("X-User-Id") String userId,
                                             @PathVariable String workspaceId
    ) {
        workspaceService.deleteWorkspace(userId,workspaceId);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusCode(HttpStatus.OK.value())
                .statusMessage("워크스페이스 회원 삭제 성공")
                .result(HttpStatus.OK)
                .build(),
                HttpStatus.OK);
    }

    // workspace 정보 반환
    @PostMapping("/return")
    public WorkspaceInfoResDto fetchWorkspaceInfo(@RequestHeader("X-User-Id")String userId, @RequestBody WorkspaceNameDto workspaceName) {
        WorkspaceInfoResDto workspaceInfoResDto =  workspaceService.fetchWorkspaceInfo(userId, workspaceName);
        return workspaceInfoResDto;
    }

    // workspace 존재 여부 확인
    @GetMapping("/{workspaceId}/exists")
    public ResponseEntity<Boolean> checkWorkspaceExists(@PathVariable String workspaceId) {
        boolean exists = workspaceService.existsById(workspaceId);
        return ResponseEntity.ok(exists);
    }

    // 워크스페이스 멤버 여부 검증
    @GetMapping("/{workspaceId}/participants/{userId}/exists")
    public ResponseEntity<Boolean> checkWorkspaceMember(
            @PathVariable String workspaceId,
            @PathVariable UUID userId
    ) {
        boolean exists = workspaceService.checkWorkspaceMember(workspaceId, userId);
        return ResponseEntity.ok(exists);
    }


    //    워크스페이스 전체 프로젝트별 마일스톤 조회
    @GetMapping("/admin/{workspaceId}")
    public ResponseEntity<?> getWorkspaceProjectProgress(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String workspaceId
    ) {
        List<ProjectProgressResDto> result = workspaceService.getWorkspaceProjectProgress(userId, workspaceId);

        return new ResponseEntity<>(
                CommonSuccessDto.builder()
                        .statusMessage("프로젝트 진행률 조회 완료")
                        .result(result)
                        .statusCode(HttpStatus.OK.value())
                        .build(),
                HttpStatus.OK
        );
    }

    // 워크스페이스 전체 프로젝트별 프로젝트 마일스톤, 스톤 목록과 스톤의 마일스톤들 조회

    @GetMapping("/admin/tree/{workspaceId}")
    public ResponseEntity<?> getProjectMileStones(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String workspaceId
    ) {
        return new ResponseEntity<>(
                CommonSuccessDto.builder()
                        .statusMessage("전체 프로젝트 마일스톤 조회 완료")
                        .result(workspaceService.milestoneListForAdmin(userId, workspaceId))
                        .statusCode(HttpStatus.OK.value())
                        .build(),
                HttpStatus.OK
        );
    }

    // 사용자 그룹별 프로젝트 현황 조회
    @GetMapping("/admin/group-progress/{workspaceId}")
    public ResponseEntity<?> getUserGroupProjectProgress(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String workspaceId
    ) {
        return new ResponseEntity<>(
                CommonSuccessDto.builder()
                        .statusMessage("사용자 그룹별 프로젝트 현황 조회 완료")
                        .result(workspaceService.getUserGroupProjectProgress(userId, workspaceId))
                        .statusCode(HttpStatus.OK.value())
                        .build(),
                HttpStatus.OK
        );
    }


}
