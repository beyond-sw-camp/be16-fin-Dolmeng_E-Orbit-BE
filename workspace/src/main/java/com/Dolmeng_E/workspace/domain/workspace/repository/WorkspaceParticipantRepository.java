package com.Dolmeng_E.workspace.domain.workspace.repository;

import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceParticipantRepository extends JpaRepository<WorkspaceParticipant, String> {
}
