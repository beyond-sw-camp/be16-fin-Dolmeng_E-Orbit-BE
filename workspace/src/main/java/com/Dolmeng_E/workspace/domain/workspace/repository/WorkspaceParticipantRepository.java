package com.Dolmeng_E.workspace.domain.workspace.repository;

import com.Dolmeng_E.workspace.domain.access_group.entity.AccessGroup;
import com.Dolmeng_E.workspace.domain.workspace.entity.Workspace;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkspaceParticipantRepository extends JpaRepository<WorkspaceParticipant, String> {

//    회원 ID 와 워크스페이스 ID로 참여자 객체 반환
    Optional<WorkspaceParticipant> findByWorkspaceIdAndUserId(String workspaceId, UUID userId);

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

}
