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

@Repository
public interface ProjectParticipantRepository extends JpaRepository<ProjectParticipant,String> {
    Boolean existsByProjectAndWorkspaceParticipant(Project project, WorkspaceParticipant workspaceParticipant);
    Optional<ProjectParticipant> findByProjectAndWorkspaceParticipant(Project project, WorkspaceParticipant workspaceParticipant);
    List<ProjectParticipant> findAllByProject(Project project);
    List<ProjectParticipant> findAllByWorkspaceParticipant(WorkspaceParticipant workspaceParticipant);

    // n+1 해결을 위한 쿼리문입니다.
    @Query("SELECT DISTINCT p FROM ProjectParticipant p " +
            "JOIN FETCH p.project pr " +
            "JOIN FETCH pr.stones s " +
            "WHERE p.workspaceParticipant = :participant " +
            "AND s.isDelete = false " +
            "AND s.status <> com.Dolmeng_E.workspace.domain.stone.entity.StoneStatus.COMPLETED")
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
}
