package com.Dolmeng_E.workspace.domain.access_group.controller;

import com.Dolmeng_E.workspace.common.domain.CommonSuccessDto;
import com.Dolmeng_E.workspace.domain.access_group.dto.DefaultAccessGroupCreateDto;
import com.Dolmeng_E.workspace.domain.access_group.service.AccessGroupService;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<?> createAdminGroup(@RequestBody DefaultAccessGroupCreateDto defaultAccessGroupCreateDto) {
//        워크스페이스 id string으로 변경시 dto에도 id string으로 변경요망
        accessGroupService.createAdminGroupForWorkspace(defaultAccessGroupCreateDto.getWorkspaceId());
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("관리자 그룹 생성 완료")
                .result(HttpStatus.CREATED)
                .statusCode(HttpStatus.CREATED.value())
                .build()
                ,HttpStatus.CREATED);
//        워크스페이스 생성자가 자동으로 관리자권한에 들어가게 포함


    }

//    일반유저 권한그룹 생성(워크스페이스 생성시 자동생성)
    @PostMapping("/common-user")
    public ResponseEntity<?> createDefaultUserGroup(@RequestBody DefaultAccessGroupCreateDto defaultAccessGroupCreateDto) {
        accessGroupService.createDefaultUserGroup(defaultAccessGroupCreateDto.getWorkspaceId());
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .statusMessage("사용자 그룹 생성 완료")
                .result(HttpStatus.CREATED)
                .statusCode(HttpStatus.CREATED.value())
                .build()
                ,HttpStatus.CREATED);
//        워크스페이스 생성자가 자동으로 사용자권한에 들어가게 포함
    }

//    커스터마이징 권한그룹 생성

//    권한그룹 수정

//    권한그룹 리스트 조회

//    권한그룹 상세 조회

//    권한그룹 사용자 추가

//    권한그룹 사용자 수정

//    권한그룹 사용자 제거

//    권한그룹 삭제

}
