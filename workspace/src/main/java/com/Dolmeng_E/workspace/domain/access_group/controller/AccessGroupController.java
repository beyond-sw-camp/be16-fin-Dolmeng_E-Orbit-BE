package com.Dolmeng_E.workspace.domain.access_group.controller;

import com.Dolmeng_E.workspace.domain.access_group.dto.AccessGroupListResDto;
import com.Dolmeng_E.workspace.domain.access_group.dto.AccessGroupModifyDto;
import com.Dolmeng_E.workspace.domain.access_group.dto.CustomAccessGroupDto;
import com.Dolmeng_E.workspace.domain.access_group.dto.DefaultAccessGroupCreateDto;
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
//        워크스페이스 생성자가 자동으로 사용자권한에 들어가게 포함로직 구현 필요
    }

//    커스터마이징 권한그룹 생성
    @PostMapping("/custom")
    public ResponseEntity<?> createCustomAccessGroup(@RequestBody CustomAccessGroupDto customAccessGroupDto) {
        accessGroupService.createCustomAccessGroup(customAccessGroupDto);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("커스텀 사용자 그룹 생성 완료")
                .result(HttpStatus.CREATED)
                .statusCode(HttpStatus.CREATED.value())
                .build()
                ,HttpStatus.CREATED);
    }

//    권한그룹 수정
    @PatchMapping("")
    public ResponseEntity<?> modifyAccessGroup(@RequestBody AccessGroupModifyDto accessGroupModifyDto, @RequestHeader("X-User-Email") String userEmail) {
        accessGroupService.modifyAccessGroup(accessGroupModifyDto, userEmail);
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
                                                 @RequestHeader("X-User-Email") String userEmail, @PathVariable String workspaceId) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("권한 그룹 리스트 조회 완료")
                .result(accessGroupService.accessGroupList(pageable, userEmail, workspaceId))
                .statusCode(HttpStatus.OK.value())
                .build()
                ,HttpStatus.OK);
    }

//    권한그룹 상세 조회

//    권한그룹 사용자 추가

//    권한그룹 사용자 수정

//    권한그룹 사용자 제거

//    권한그룹 삭제

}
