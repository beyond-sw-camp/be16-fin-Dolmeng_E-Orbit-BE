package com.Dolmeng_E.workspace.domain.workspace.repository;

import com.Dolmeng_E.workspace.domain.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, String> {
    Optional<Workspace> findByIdAndIsDeleteFalse(String workspaceId);
}
