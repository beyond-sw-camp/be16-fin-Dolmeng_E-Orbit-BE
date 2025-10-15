package com.Dolmeng_E.workspace.domain.stone.repository;

import com.Dolmeng_E.workspace.domain.project.entity.Project;
import com.Dolmeng_E.workspace.domain.stone.entity.Stone;
import com.Dolmeng_E.workspace.domain.stone.entity.StoneParticipant;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoneParticipantRepository extends JpaRepository<StoneParticipant, String> {
    List<StoneParticipant> findAllByStone(Stone stone);
    Boolean existsByStoneAndWorkspaceParticipant(Stone stone, WorkspaceParticipant workspaceParticipant);
    Optional<StoneParticipant> findByStoneAndWorkspaceParticipant(Stone stone, WorkspaceParticipant workspaceParticipant);
    boolean existsByStone_ProjectAndWorkspaceParticipant(Project project, WorkspaceParticipant participant);

}
