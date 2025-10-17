package com.Dolmeng_E.workspace.domain.workspace.repository;

import com.Dolmeng_E.workspace.domain.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, String> {
    Optional<Workspace> findByIdAndIsDeleteFalse(String workspaceId);

    Workspace findByUserIdAndWorkspaceName(UUID userId, String workspaceName);

    // 워크스페이스 존재 여부 확인
    boolean existsById(String workspaceId);
}
