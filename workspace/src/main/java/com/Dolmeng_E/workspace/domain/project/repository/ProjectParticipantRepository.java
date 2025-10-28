package com.Dolmeng_E.workspace.domain.project.repository;

import com.Dolmeng_E.workspace.domain.project.entity.Project;
import com.Dolmeng_E.workspace.domain.project.entity.ProjectParticipant;
import com.Dolmeng_E.workspace.domain.workspace.entity.Workspace;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectParticipantRepository extends JpaRepository<ProjectParticipant,String> {
    Boolean existsByProjectAndWorkspaceParticipant(Project project, WorkspaceParticipant workspaceParticipant);
    Optional<ProjectParticipant> findByProjectAndWorkspaceParticipant(Project project, WorkspaceParticipant workspaceParticipant);
    List<ProjectParticipant> findAllByProject(Project project);
    List<ProjectParticipant> findAllByWorkspaceParticipant(WorkspaceParticipant workspaceParticipant);

    @Query("""
    SELECT DISTINCT p FROM ProjectParticipant p
    JOIN FETCH p.project pr
    LEFT JOIN FETCH pr.stones s
    WHERE p.workspaceParticipant = :participant
    AND pr.isDelete = false
    AND (s.isDelete = false OR s IS NULL)
    AND (s.status <> com.Dolmeng_E.workspace.domain.stone.entity.StoneStatus.COMPLETED OR s IS NULL)
    """)
    List<ProjectParticipant> findAllWithStonesByWorkspaceParticipant(
            @Param("participant") WorkspaceParticipant participant
    );

    // stone이 없어도 조회 가능
    @Query("SELECT DISTINCT p FROM ProjectParticipant p " +
            "JOIN FETCH p.project pr " +
            "LEFT JOIN FETCH pr.stones s " +
            "WHERE p.workspaceParticipant = :participant " +
            "AND pr.isDelete = false " +
            "AND (s.isDelete = false OR s IS NULL) " +
            "AND (s.status <> com.Dolmeng_E.workspace.domain.stone.entity.StoneStatus.COMPLETED OR s IS NULL)")
    List<ProjectParticipant> findAllWithOptionalStonesByWorkspaceParticipant(
            @Param("participant") WorkspaceParticipant participant
    );

    @Query("SELECT p.project FROM ProjectParticipant p " +
            "WHERE p.workspaceParticipant = :participant " +
            "AND p.project.isDelete = false " +
            "ORDER BY p.project.startTime DESC")
    Optional<Project> findLatestProjectByParticipant(@Param("participant") WorkspaceParticipant participant);

    List<ProjectParticipant> findAllByWorkspaceParticipantIn(List<WorkspaceParticipant> participants);

    @Query("""
    SELECT DISTINCT p.project
    FROM ProjectParticipant p
    JOIN p.workspaceParticipant wp
    WHERE wp.workspace.id = :workspaceId
      AND wp.userId = :userId
      AND p.project.isDelete = false
      AND p.project.projectStatus <> com.Dolmeng_E.workspace.domain.project.entity.ProjectStatus.STORAGE
    """)
    List<Project> findProjectsByUserInWorkspace(
            @Param("userId") UUID userId,
            @Param("workspaceId") String workspaceId
    );
}