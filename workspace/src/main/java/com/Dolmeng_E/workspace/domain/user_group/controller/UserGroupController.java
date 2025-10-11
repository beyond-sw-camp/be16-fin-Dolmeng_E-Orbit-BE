package com.Dolmeng_E.workspace.domain.user_group.controller;

import com.Dolmeng_E.workspace.domain.user_group.dto.*;
import com.Dolmeng_E.workspace.domain.user_group.service.UserGroupService;
import com.example.modulecommon.dto.CommonSuccessDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user-groups")
@RequiredArgsConstructor
public class UserGroupController {
    private final UserGroupService userGroupService;

    // 사용자 그룹 생성

    @PostMapping("")
    public ResponseEntity<?> createUserGroup(
            @RequestHeader("X-User-Email") String userEmail,
            @RequestBody UserGroupCreateDto dto
    ) {
        String groupId = userGroupService.createUserGroup(userEmail,dto);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("기본 사용자 그룹 생성 완료")
                .result("group id : " + groupId)
                .statusCode(HttpStatus.CREATED.value())
                .build()
                ,HttpStatus.CREATED);
    }

    // 사용자 그룹 목록 조회
    @GetMapping("")
    public ResponseEntity<?> getUserGroupList(
            @RequestHeader("X-User-Email") String userEmail,
            @RequestParam("workspaceId") String workspaceId,
            @PageableDefault(size = 8, sort = "createdAt") Pageable pageable
    ) {
        Page<UserGroupListResDto> userGroups = userGroupService.getUserGroupList(userEmail, workspaceId, pageable);

        return ResponseEntity.ok(CommonSuccessDto.builder()
                .statusMessage("사용자 그룹 목록 조회 완료")
                .result(userGroups)
                .statusCode(HttpStatus.OK.value())
                .build());

    }
    // 사용자 그룹에 추가
    @PostMapping("/{groupId}/users")
    public ResponseEntity<?> addUsersToGroup(
            @RequestHeader("X-User-Email") String userEmail,
            @PathVariable String groupId,
            @RequestBody UserGroupAddUserDto dto
    ) {
        userGroupService.addUsersToGroup(userEmail, groupId, dto);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("사용자 그룹에 사용자 추가 완료")
                .result(HttpStatus.CREATED)
                .statusCode(HttpStatus.CREATED.value())
                .build(),
                HttpStatus.CREATED);
    }



    // 사용자 그룹 상세 조회
    @GetMapping("/{groupId}")
    public ResponseEntity<?> getUserGroupDetail(
            @RequestHeader("X-User-Email") String userEmail,
            @PathVariable String groupId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        UserGroupDetailResDto resDto = userGroupService.getUserGroupDetail(userEmail, groupId, pageable);

        return ResponseEntity.ok(CommonSuccessDto.builder()
                .statusMessage("사용자 그룹 상세 조회 완료")
                .statusCode(HttpStatus.OK.value())
                .result(resDto)
                .build());
    }



    // 사용자 그룹에서 삭제
    @DeleteMapping("/{groupId}/users")
    public ResponseEntity<?> removeUsersFromGroup(
            @RequestHeader("X-User-Email") String userEmail,
            @PathVariable String groupId,
            @RequestBody UserGroupRemoveUserDto dto
    ) {
        userGroupService.removeUsersFromGroup(userEmail, groupId, dto);

        return ResponseEntity.ok(CommonSuccessDto.builder()
                .statusCode(HttpStatus.OK.value())
                .statusMessage("사용자 그룹에서 사용자 삭제 완료")
                .result("groupId: " + groupId + ", removedCount: " + dto.getUserIdList().size())
                .build());
    }

    // 사용자 그룹 삭제
    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteUserGroup(
            @RequestHeader("X-User-Email") String userEmail,
            @PathVariable String groupId
    ) {
        userGroupService.deleteUserGroup(userEmail, groupId);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("사용자 그룹 삭제 완료")
                .result("group id : " + groupId)
                .statusCode(HttpStatus.OK.value())
                .build()
                , HttpStatus.OK);
    }

}
