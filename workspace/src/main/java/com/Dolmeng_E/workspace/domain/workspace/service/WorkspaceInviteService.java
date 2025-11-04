package com.Dolmeng_E.workspace.domain.workspace.service;

import com.Dolmeng_E.workspace.common.service.UserFeign;
import com.Dolmeng_E.workspace.domain.workspace.dto.InviteRequestListDto;
import com.Dolmeng_E.workspace.domain.workspace.dto.WorkspaceAddUserDto;
import com.Dolmeng_E.workspace.domain.workspace.dto.WorkspaceInviteEvent;
import com.Dolmeng_E.workspace.domain.workspace.entity.*;
import com.Dolmeng_E.workspace.domain.workspace.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class WorkspaceInviteService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceInviteRepository inviteRepository;
    private final WorkspaceParticipantRepository participantRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final WorkspaceService workspaceService;
    private final UserFeign userFeign;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // 1. 초대 생성 및 삭제된 사용자 복구
    @Transactional
    public void sendInviteList(String userId, String workspaceId, List<String> emailList) {

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스를 찾을 수 없습니다."));

        WorkspaceParticipant inviter = participantRepository
                .findByWorkspaceIdAndUserId(workspaceId, UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        if (!inviter.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            throw new IllegalArgumentException("워크스페이스 관리자만 초대할 수 있습니다.");
        }

        for (String email : emailList) {

            // 1. 삭제된 사용자 여부 확인
            Optional<WorkspaceParticipant> deletedParticipantOpt = participantRepository
                    .findByWorkspaceId(workspace.getId()).stream()
                    .filter(p -> p.isDelete() && p.getUserName().equalsIgnoreCase(email))
                    .findFirst();

            if (deletedParticipantOpt.isPresent()) {
                WorkspaceParticipant deletedParticipant = deletedParticipantOpt.get();
                deletedParticipant.restoreParticipant(); // isDelete=false로 복구

                // 복구 후에도 새 초대코드 발급 (재초대)
                String token = UUID.randomUUID().toString();
                WorkspaceInvite invite = WorkspaceInvite.builder()
                        .email(email)
                        .inviteToken(token)
                        .workspace(workspace)
                        .inviter(inviter)
                        .expiredAt(LocalDateTime.now().plusHours(24))
                        .isUsed(false)
                        .build();
                inviteRepository.save(invite);

                eventPublisher.publishEvent(WorkspaceInviteEvent
                        .builder()
                        .email(email)
                        .workspaceName(workspace.getWorkspaceName())
                        .token(token)
                        .build()
                );
                continue;
            }

            // 2. 이미 초대된 이메일은 스킵
            if (inviteRepository.existsByWorkspaceAndEmail(workspace, email)) continue;

            // 3. 새 초대 생성
            String token = UUID.randomUUID().toString();

            WorkspaceInvite invite = WorkspaceInvite.builder()
                    .email(email)
                    .inviteToken(token)
                    .workspace(workspace)
                    .inviter(inviter)
                    .expiredAt(LocalDateTime.now().plusHours(24))
                    .isUsed(false)
                    .build();

            inviteRepository.save(invite);

            // 4. 커밋 후 메일 발송 이벤트 발행
            eventPublisher.publishEvent(WorkspaceInviteEvent
                    .builder()
                            .email(email)
                            .workspaceName(workspace.getWorkspaceName())
                            .token(token)
                    .build()
            );
        }
    }

    @Transactional
    public void acceptInvite(String userId, String token) {

        // 1. 초대 토큰 검증
        WorkspaceInvite invite = inviteRepository.findByInviteToken(token)
                .orElseThrow(() -> new EntityNotFoundException("유효하지 않은 초대코드입니다."));

        if (invite.getIsUsed()) throw new IllegalArgumentException("이미 사용된 초대코드입니다.");
        if (invite.getExpiredAt().isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("초대코드가 만료되었습니다.");

        Workspace workspace = invite.getWorkspace();

        // 2. 워크스페이스 추가 처리 (addParticipants 호출)
        WorkspaceAddUserDto dto = WorkspaceAddUserDto.builder()
                .userIdList(List.of(UUID.fromString(userId)))
                .build();

        workspaceService.addParticipants(invite.getInviter().getUserId().toString(), // 초대한 관리자
                workspace.getId(),
                dto
        );

        // 3. 초대 상태 갱신
        if (invite.getIsUsed()) throw new IllegalArgumentException("이미 사용된 초대코드입니다.");
        invite.setIsUsed(true);
    }
}
