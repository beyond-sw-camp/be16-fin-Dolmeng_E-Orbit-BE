package com.Dolmeng_E.workspace.domain.workspace.repository;

import com.Dolmeng_E.workspace.domain.access_group.entity.AccessGroup;
import com.Dolmeng_E.workspace.domain.workspace.entity.Workspace;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceParticipantRepository extends JpaRepository<WorkspaceParticipant, String> {

    // 워크스페이스+유저로 참여자 1명 조회 (서비스에서 많이 사용)
    Optional<WorkspaceParticipant> findByWorkspaceIdAndUserId(String workspaceId, UUID userId);

    // 활성(삭제되지 않은) 참여자 수 - 파생 쿼리
    long countByWorkspace_IdAndIsDeleteFalse(String workspaceId);

    // ✅ 호환 유지용: 기존에 사용 중인 메서드 이름 유지 (JPQL 없이 파생 쿼리로 위임)
    default long countActiveByWorkspaceId(String workspaceId) {
        return countByWorkspace_IdAndIsDeleteFalse(workspaceId);
    }

    int countByAccessGroup(AccessGroup accessGroup);

    List<WorkspaceParticipant> findByAccessGroup(AccessGroup accessGroup);

    List<WorkspaceParticipant> findByWorkspaceId(String workspaceId);

    List<WorkspaceParticipant> findByUserIdAndIsDeleteFalse(UUID userId);

    long countByWorkspaceIdAndIsDeleteFalse(String workspaceId);

    // 페이지네이션 메소드와 오버로딩 되어 리팩토링
    List<WorkspaceParticipant> findAllByWorkspace(Workspace workspace);

    // 워크스페이스 멤버 검증 여부
    boolean existsByWorkspaceIdAndUserId(String workspaceId, UUID userId);

    Page<WorkspaceParticipant> findAllByWorkspaceId(String workspaceId, Pageable pageable);

    List<WorkspaceParticipant> findByWorkspaceIdAndIsDeleteFalse(String workspaceId);

    List<WorkspaceParticipant> findByWorkspaceAndAccessGroup(Workspace workspace, AccessGroup accessGroup);

    List<WorkspaceParticipant> findAllByUserId(UUID userId);

    WorkspaceParticipant findByWorkspaceIdAndWorkspaceRole(String workspaceId, WorkspaceRole workspaceRole);
}
