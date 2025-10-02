package com.Dolmeng_E.workspace.domain.workspace.controller;

import com.Dolmeng_E.workspace.common.domain.CommonSuccessDto;
import com.Dolmeng_E.workspace.domain.workspace.dto.WorkspaceCreateDto;
import com.Dolmeng_E.workspace.domain.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/workspace")
@RequiredArgsConstructor
public class WorkspaceController {
    private final WorkspaceService workspaceService;

//    새 워크스페이스 생성
    @PostMapping("")
    public ResponseEntity<?> createWorkspace(@RequestBody WorkspaceCreateDto workspaceCreateDto, @RequestHeader("X-User-Email") String userEmail) {
        Long workspaceId = workspaceService.createWorkspace(workspaceCreateDto, userEmail);
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(workspaceId)
                .statusCode(HttpStatus.CREATED.value())
                .statusMessage("워크스페이스 생성 성공")
                .build(),
                HttpStatus.CREATED);
    }

//    회원가입 시 워크스페이스 생성

//    워크스페이스 목록 조회

//    워크스페이스 수정

//    워크스페이스 변경
}
