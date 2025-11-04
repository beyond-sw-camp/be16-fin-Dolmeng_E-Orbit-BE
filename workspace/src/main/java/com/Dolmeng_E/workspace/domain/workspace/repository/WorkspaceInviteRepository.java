package com.Dolmeng_E.workspace.domain.workspace.repository;

import com.Dolmeng_E.workspace.domain.workspace.entity.Workspace;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkspaceInviteRepository extends JpaRepository<WorkspaceInvite, Long> {
    Optional<WorkspaceInvite> findByInviteToken(String token);
    boolean existsByWorkspaceAndEmail(Workspace workspace, String email);
}
