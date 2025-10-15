package com.Dolmeng_E.workspace.domain.project.repository;

import com.Dolmeng_E.workspace.domain.project.entity.Project;
import com.Dolmeng_E.workspace.domain.project.entity.ProjectParticipant;
import com.Dolmeng_E.workspace.domain.workspace.entity.Workspace;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectParticipantRepository extends JpaRepository<ProjectParticipant,String> {
    Boolean existsByProjectAndWorkspaceParticipant(Project project, WorkspaceParticipant workspaceParticipant);
    Optional<ProjectParticipant> findByProjectAndWorkspaceParticipant(Project project, WorkspaceParticipant workspaceParticipant);
    List<ProjectParticipant> findAllByProject(Project project);
}
