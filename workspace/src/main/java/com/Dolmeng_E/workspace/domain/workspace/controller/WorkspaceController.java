package com.Dolmeng_E.workspace.domain.workspace.controller;

import com.Dolmeng_E.workspace.common.dto.*;
import com.Dolmeng_E.workspace.domain.project.dto.ProjectProgressResDto;
import com.Dolmeng_E.workspace.domain.workspace.dto.*;
import com.Dolmeng_E.workspace.domain.workspace.service.WorkspaceService;
import com.example.modulecommon.dto.CommonSuccessDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Set;
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

//    워크스페이스 변경
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


//    워크스페이스 참여자 목록 조회
    @GetMapping("/{workspaceId}/participants")
    public ResponseEntity<?> getWorkspaceParticipants(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String workspaceId,
            @PageableDefault(size = 8, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusCode(HttpStatus.OK.value())
                .statusMessage("워크스페이스 참여자 목록 조회 성공")
                .result(workspaceService.getWorkspaceParticipants(userId, workspaceId, pageable))
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

    // 워크스페이스 삭제
    @DeleteMapping("/{workspaceId}")
    public ResponseEntity<?> deleteWorkspace(@RequestHeader("X-User-Id") String userId,
                                             @PathVariable String workspaceId
    ) {
        workspaceService.deleteWorkspace(userId,workspaceId);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusCode(HttpStatus.OK.value())
                .statusMessage("워크스페이스 삭제 성공")
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

    // 워크스페이스에 존재하지 않는 회원 목록에서 검색
    @PostMapping("/participants/search-outside")
    public ResponseEntity<?> searchUsersNotInWorkspace(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody SearchDto dto
    ) {
        return new ResponseEntity<>(
                CommonSuccessDto.builder()
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("워크스페이스 외부 사용자 검색 성공")
                        .result(workspaceService.searchParticipants(userId, dto))
                        .build(),
                HttpStatus.OK
        );
    }

    // 워크스페이스 내 참여자 검색
    @PostMapping("/participants/search")
    public ResponseEntity<?> searchWorkspaceParticipants(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody SearchDto dto
    ) {
        return new ResponseEntity<>(
                CommonSuccessDto.builder()
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("워크스페이스 내부 사용자 검색 성공")
                        .result(workspaceService.searchWorkspaceParticipants(userId, dto))
                        .build(),
                HttpStatus.OK
        );
    }

    // 워크스페이스 내 사용자 그룹이 없는 참여자 검색(사용자 그룹 추가시 활용)
    @PostMapping("/participants/not-in-groups/search")
    public ResponseEntity<?> searchWorkspaceParticipantsNotInUserGroup(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody SearchDto dto
    ) {
        return new ResponseEntity<>(
                CommonSuccessDto.builder()
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("워크스페이스 내부 사용자 검색 성공")
                        .result(workspaceService.searchWorkspaceParticipantsNotInUserGroup(userId, dto))
                        .build(),
                HttpStatus.OK
        );
    }

    // 권한그룹이 없는 워크스페이스 참여자 조회
    @PostMapping("/participants/not-in-access-groups/search")
    public ResponseEntity<?> searchWorkspaceParticipantsNotInAccessGroup(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody SearchDto dto
    ) {
        return new ResponseEntity<>(
                CommonSuccessDto.builder()
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("권한그룹 미소속 사용자 조회 성공")
                        .result(workspaceService.searchWorkspaceParticipantsNotInAccessGroup(userId, dto))
                        .build(),
                HttpStatus.OK
        );
    }

    // 특정 워크스페이스 내 내 Task 목록 조회
    @GetMapping("/{workspaceId}/my-tasks")
    public ResponseEntity<?> getMyTasksInWorkspace(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String workspaceId
    ) {
        List<MyTaskResDto> myTasks = workspaceService.getMyTasksInWorkspace(userId, workspaceId);

        return new ResponseEntity<>(
                CommonSuccessDto.builder()
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("워크스페이스별 내 태스크 목록 조회 성공")
                        .result(myTasks)
                        .build(),
                HttpStatus.OK
        );
    }

    // 특정 워크스페이스 내 내 프로젝트 목록 조회
    @GetMapping("/{workspaceId}/my-projects")
    public ResponseEntity<?> getMyProjectsInWorkspace(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String workspaceId
    ) {
        List<MyProjectResDto> myProjects = workspaceService.getMyProjectsInWorkspace(userId, workspaceId);

        return new ResponseEntity<>(
                CommonSuccessDto.builder()
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("워크스페이스별 내 프로젝트 목록 조회 성공")
                        .result(myProjects)
                        .build(),
                HttpStatus.OK
        );
    }


    // 워크스페이스 담당자 확인
    @GetMapping("/{workspaceId}/manager/check")
    ResponseEntity<CommonSuccessDto> checkWorkspaceManager(
            @PathVariable("workspaceId") String workspaceId,
            @RequestHeader("X-User-Id") String userId
    ) {
        boolean isWorkspaceParticipant = workspaceService.checkWorkspaceManager(workspaceId,userId);
        return new ResponseEntity<>(
                CommonSuccessDto.builder()
                        .result(isWorkspaceParticipant)
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("워크스페이스 담당자 정보입니다.")
                        .build(),
                HttpStatus.OK
        );
    }


    // 워크스페이스 참여자 확인
    @GetMapping("/workspace/{workspaceId}/members/check")
    ResponseEntity<CommonSuccessDto> checkWorkspaceMembership(
            @PathVariable("workspaceId") String workspaceId,
            @RequestHeader("X-User-Id") String userId
    ) {
        boolean isWorkspaceParticipant = workspaceService.checkWorkspaceMembership(workspaceId,userId);
        return new ResponseEntity<>(
                CommonSuccessDto.builder()
                        .result(isWorkspaceParticipant)
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("워크스페이스 참여자 정보입니다.")
                        .build(),
                HttpStatus.OK
        );

    }

    // 프로젝트 담당자 확인
    @GetMapping("/project/{projectId}/manager/check")
    public ResponseEntity<CommonSuccessDto> checkProjectManagership(
            @PathVariable("projectId") String projectId,
            @RequestHeader("X-User-Id") String userId
    ) {
        boolean result = workspaceService.checkProjectManagership(projectId, userId);
        return new ResponseEntity<>(
                CommonSuccessDto.builder()
                        .result(result)
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("프로젝트 담당자 여부입니다.")
                        .build(),
                HttpStatus.OK
        );
    }

    // 프로젝트 참여자 확인
    @GetMapping("/workspace/project/{projectId}/members/check")
    public ResponseEntity<CommonSuccessDto> checkProjectMembership(
            @PathVariable("projectId") String projectId,
            @RequestHeader("X-User-Id") String userId
    ) {
        boolean result = workspaceService.checkProjectMembership(projectId, userId);
        return new ResponseEntity<>(
                CommonSuccessDto.builder()
                        .result(result)
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("프로젝트 참여자 여부입니다.")
                        .build(),
                HttpStatus.OK
        );
    }

    // 스톤 담당자 확인
    @GetMapping("/stone/{stoneId}/manager/check")
    public ResponseEntity<CommonSuccessDto> checkStoneManagership(
            @PathVariable("stoneId") String stoneId,
            @RequestHeader("X-User-Id") String userId
    ) {
        boolean result = workspaceService.checkStoneManagership(stoneId, userId);
        return new ResponseEntity<>(
                CommonSuccessDto.builder()
                        .result(result)
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("스톤 담당자 여부입니다.")
                        .build(),
                HttpStatus.OK
        );
    }

    // 스톤 참여자 확인
    @GetMapping("/stone/{stoneId}/members/check")
    public ResponseEntity<CommonSuccessDto> checkStoneMembership(
            @PathVariable("stoneId") String stoneId,
            @RequestHeader("X-User-Id") String userId
    ) {
        boolean result = workspaceService.checkStoneMembership(stoneId, userId);
        return new ResponseEntity<>(
                CommonSuccessDto.builder()
                        .result(result)
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("스톤 참여자 여부입니다.")
                        .build(),
                HttpStatus.OK
        );
    }

    // workspaceId 넘겼을 때 하위 프로젝트 Id, 프로젝트명 가져오는 api
    @GetMapping("/project/{workspaceId}/sub-project")
    public ResponseEntity<List<SubProjectResDto>> getSubProjects(@PathVariable String workspaceId) {
        List<SubProjectResDto> projects = workspaceService.getSubProjectsByWorkspace(workspaceId);
        return ResponseEntity.ok(projects);
    }

    //projectId 넘겼을 때 하위 스톤 id, 테스크명 가져오는 api
    @GetMapping("/stone/{projectId}/sub-stone-task")
    public ResponseEntity<StoneTaskResDto> getSubStonesAndTasks(@PathVariable String projectId) {
        StoneTaskResDto response = workspaceService.getSubStonesAndTasks(projectId);
        return ResponseEntity.ok(response);
    }

    // stoneId 와 userId 넘겼을 때 웤스 관리자인지 프로젝트관리자인지 확인하는 api
    @GetMapping("/work-project/{stoneId}/manager/check")
    public WorkspaceOrProjectManagerCheckDto checkWorkspaceOrProjectManager(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable("stoneId") String stoneId
    ) {
        return workspaceService.checkWorkspaceOrProjectManager(stoneId, userId);
    }

    // 워크스페이스id, 프젝id, 스톤id 중 하나 넘겼을 때 해당 이름 받아오는 api
    @PostMapping("/entity/name")
    public EntityNameResDto getEntityName(
            @RequestBody EntityNameReqDto dto
    ) {
        return workspaceService.getEntityName(dto);
    }

    // 워크스페이스에서 나의 스톤 목록 가져오기 (루트스톤 제외)
    @GetMapping("/{workspaceId}/my-stones")
    public ResponseEntity<?> getMyStonesInWorkspace(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String workspaceId
    ) {
        List<MyStoneResDto> result = workspaceService.getMyStonesInWorkspace(userId, workspaceId);

        return new ResponseEntity<>(
                CommonSuccessDto.builder()
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("워크스페이스 내 내 스톤 목록 조회 성공")
                        .result(result)
                        .build(),
                HttpStatus.OK
        );
    }

    // 접근 가능한 유저 목록 가져오기
    @GetMapping("/{rootId}/{rootType}/getViewableUserIds")
    public Set<String> getViewableUserIds(@PathVariable String rootId, @PathVariable String rootType) {
        return workspaceService.getViewableUserIds(rootId, rootType);
    }






}
