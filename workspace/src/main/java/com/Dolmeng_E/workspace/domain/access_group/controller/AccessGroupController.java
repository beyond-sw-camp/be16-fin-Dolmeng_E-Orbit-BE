package com.Dolmeng_E.workspace.domain.access_group.controller;

import com.Dolmeng_E.workspace.domain.access_group.dto.*;
import com.Dolmeng_E.workspace.domain.access_group.service.AccessGroupService;
import com.example.modulecommon.dto.CommonSuccessDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/access")
public class AccessGroupController {

    private final AccessGroupService accessGroupService;

    // 관리자 권한 그룹 생성 (워크스페이스 ID 기반, 워크스페이스 생성시 자동생성)
    @PostMapping("/admin")
    public ResponseEntity<?> createAdminAccessGroup(@RequestBody DefaultAccessGroupCreateDto defaultAccessGroupCreateDto) {
        String id = accessGroupService.createAdminGroupForWorkspace(defaultAccessGroupCreateDto.getWorkspaceId());
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("관리자 그룹 생성 완료")
                .result(id)
                .statusCode(HttpStatus.CREATED.value())
                .build()
                ,HttpStatus.CREATED);
//        워크스페이스 생성자가 자동으로 관리자권한에 들어가게 포함


    }

//    일반유저 권한그룹 생성(워크스페이스 생성시 자동생성)
    @PostMapping("/common-user")
    public ResponseEntity<?> createDefaultUserAccessGroup(@RequestBody DefaultAccessGroupCreateDto defaultAccessGroupCreateDto) {
        String id = accessGroupService.createDefaultUserAccessGroup(defaultAccessGroupCreateDto.getWorkspaceId());
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("기본 사용자 그룹 생성 완료")
                .result(id)
                .statusCode(HttpStatus.CREATED.value())
                .build()
                ,HttpStatus.CREATED);
    }

//    커스터마이징 권한그룹 생성
    @PostMapping("/custom")
    public ResponseEntity<?> createCustomAccessGroup(@RequestBody CustomAccessGroupDto customAccessGroupDto, @RequestHeader("X-User-Id") String userId) {
        accessGroupService.createCustomAccessGroup(customAccessGroupDto, userId);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("커스텀 사용자 그룹 생성 완료")
                .result(HttpStatus.CREATED)
                .statusCode(HttpStatus.CREATED.value())
                .build()
                ,HttpStatus.CREATED);
    }

//    권한그룹 수정
    @PatchMapping("")
    public ResponseEntity<?> modifyAccessGroup(@RequestBody AccessGroupModifyDto accessGroupModifyDto, @RequestHeader("X-User-Id") String userId) {
        accessGroupService.modifyAccessGroup(accessGroupModifyDto, userId);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("권한 그룹 수정 완료")
                .result(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .build()
                ,HttpStatus.OK);
    }
//    권한그룹 리스트 조회
    @GetMapping("/group-list/{workspaceId}")
        public ResponseEntity<?> accessGroupList(@PageableDefault(page = 0, size = 10) Pageable pageable,
                                                 @RequestHeader("X-User-Id") String userId, @PathVariable String workspaceId) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("권한 그룹 리스트 조회 완료")
                .result(accessGroupService.accessGroupList(pageable, userId, workspaceId))
                .statusCode(HttpStatus.OK.value())
                .build()
                ,HttpStatus.OK);
    }

//    권한그룹 상세 조회
    @GetMapping("/group-detail/{groupId}")
    public ResponseEntity<?> getAccessGroupDetail(@RequestHeader("X-User-Id") String userId, @PathVariable String groupId) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("권한 그룹 상세 조회 완료")
                .result(accessGroupService.getAccessGroupDetail(userId,groupId))
                .statusCode(HttpStatus.OK.value())
                .build()
                ,HttpStatus.OK);
    }


//    권한그룹 사용자 추가(워크스페이스 초대 받아서 가입 시 디폴트로 일반사용자 권한그룹에 추가되어 이거를 써야할지)
    @PostMapping("/{groupId}/users")
        public ResponseEntity<?> addUserToAccessGroup(@RequestHeader("X-User-Id") String userId
            , @PathVariable String groupId, AccessGroupAddUserDto accessGroupAddUserDto) {
        accessGroupService.addUserToAccessGroup(userId,groupId,accessGroupAddUserDto);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("권한그룹 사용자 추가 완료")
                .result(HttpStatus.CREATED)
                .statusCode(HttpStatus.CREATED.value())
                .build()
                ,HttpStatus.CREATED);
    }

    //    권한그룹 사용자 변경 (이미 워크스페이스에 존재하는 사용자의 그룹 변경 - A그룹에 있던 유저를 B그룹으로 옮겨라)
    @PatchMapping("/{groupId}/users")
    public ResponseEntity<?> updateUserAccessGroup(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String groupId,
            @RequestBody AccessGroupAddUserDto dto
    ) {
        accessGroupService.updateUserAccessGroup(userId, groupId, dto);

        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("권한그룹 사용자 변경 완료")
                .result(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .build(),
                HttpStatus.OK);
    }


//    권한그룹 사용자 이동(일반사용자 그룹으로 이동 - 이 그룹(A)에 있던 유저를 일반 그룹으로 돌려라 )
    @PatchMapping("/{groupId}/move")
    public ResponseEntity<?> moveUserAccessGroup(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String groupId,
            @RequestBody AccessGroupMoveDto dto
    ) {
        accessGroupService.moveUserAccessGroup(userId, groupId, dto);

        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("권한그룹 사용자 변경 완료")
                .result(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .build(),
                HttpStatus.OK);

    }
//    권한그룹 삭제
    @DeleteMapping("/{groupId}/delete")
    public ResponseEntity<?> deleteUserAccessGroup(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String groupId
    ) {
        accessGroupService.deleteUserAccessGroup(userId,groupId);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("권한그룹 사용자 삭세 완료")
                .result(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .build(),
                HttpStatus.OK);
    }

}
