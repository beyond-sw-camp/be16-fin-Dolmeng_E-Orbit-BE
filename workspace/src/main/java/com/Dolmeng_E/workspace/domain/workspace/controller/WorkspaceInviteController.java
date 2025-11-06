package com.Dolmeng_E.workspace.domain.workspace.controller;

import com.Dolmeng_E.workspace.domain.workspace.dto.InviteAcceptDto;
import com.Dolmeng_E.workspace.domain.workspace.dto.InviteRequestListDto;
import com.Dolmeng_E.workspace.domain.workspace.service.WorkspaceInviteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/workspace/invite")
public class WorkspaceInviteController {

    private final WorkspaceInviteService inviteService;

    // 여러 명 초대
    @PostMapping("/{workspaceId}")
    public ResponseEntity<?> sendInviteList(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String workspaceId,
            @RequestBody InviteRequestListDto dto) {

        inviteService.sendInviteList(userId, workspaceId, dto.getEmailList());
        return ResponseEntity.ok("초대 메일이 발송되었습니다.");
    }

    // 초대 수락
    @PostMapping("/accept")
    public ResponseEntity<?> acceptInvite(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody InviteAcceptDto dto) {
        inviteService.acceptInvite(userId, dto.getToken());
        return ResponseEntity.ok("워크스페이스에 참여되었습니다.");
    }
}
