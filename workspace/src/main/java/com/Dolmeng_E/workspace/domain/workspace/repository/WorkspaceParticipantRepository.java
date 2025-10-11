package com.Dolmeng_E.workspace.domain.workspace.repository;

import com.Dolmeng_E.workspace.domain.access_group.entity.AccessGroup;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
