package com.Dolmeng_E.user.domain.sharedCalendar.service;

import com.Dolmeng_E.user.common.service.WorkspaceFeign;
import com.Dolmeng_E.user.domain.user.entity.User;
import com.Dolmeng_E.user.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

// 일정 검증 로직
@Component
@RequiredArgsConstructor
@Slf4j
public class CalendarValidationService {

    private final UserRepository userRepository;
    private final WorkspaceFeign workspaceFeign;

    /** 유저 존재 여부 검증 */
    public User validateUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
    }

    /** 워크스페이스 존재 여부 검증 */
    public void validateWorkspace(String workspaceId) {
        if (!workspaceFeign.checkWorkspaceExists(workspaceId)) {
            throw new IllegalArgumentException("존재하지 않는 워크스페이스입니다.");
        }
    }

    /** 유저의 워크스페이스 소속 여부 검증 */
    public void validateMember(String workspaceId, UUID userId) {
        if (!workspaceFeign.checkWorkspaceMembership(workspaceId, userId)) {
            throw new IllegalArgumentException("해당 유저는 워크스페이스에 속하지 않습니다.");
        }
    }

    /** 종합 검증 (유저 + 워크스페이스 + 소속) */
    public User validateUserAndWorkspace(UUID userId, String workspaceId) {
        User user = validateUser(userId);
        validateWorkspace(workspaceId);
        validateMember(workspaceId, userId);
        return user;
    }

    public void safeValidateWorkspace(String workspaceId) {
        try {
            if (!workspaceFeign.checkWorkspaceExists(workspaceId)) {
                log.warn("⚠️ 존재하지 않는 워크스페이스 ID: {}", workspaceId);
            }
        } catch (Exception e) {
            log.error("⚠️ 워크스페이스 검증 실패 (Feign 오류): {}", e.getMessage());
        }
    }
}
