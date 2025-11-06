package com.Dolmeng_E.workspace.domain.stone.repository;

import com.Dolmeng_E.workspace.domain.project.entity.Project;
import com.Dolmeng_E.workspace.domain.stone.entity.Stone;
import com.Dolmeng_E.workspace.domain.stone.entity.StoneParticipant;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoneParticipantRepository extends JpaRepository<StoneParticipant, String> {
    List<StoneParticipant> findAllByStone(Stone stone);
    Boolean existsByStoneAndWorkspaceParticipant(Stone stone, WorkspaceParticipant workspaceParticipant);
    Optional<StoneParticipant> findByStoneAndWorkspaceParticipant(Stone stone, WorkspaceParticipant workspaceParticipant);
    boolean existsByStone_ProjectAndWorkspaceParticipant(Project project, WorkspaceParticipant participant);

    // n+1 을 해결하기 위한 쿼리문 - 내가 참여 중인 스톤들만 미리 캐싱
    @Query("""
    SELECT DISTINCT sp
    FROM StoneParticipant sp
    JOIN FETCH sp.stone s
    JOIN FETCH s.project p
    WHERE sp.workspaceParticipant = :participant
    AND s.isDelete = false
    AND s.status <> com.Dolmeng_E.workspace.domain.stone.entity.StoneStatus.COMPLETED
    """)
    List<StoneParticipant> findAllActiveWithStoneByWorkspaceParticipant(@Param("participant") WorkspaceParticipant participant);

    boolean existsByWorkspaceParticipantAndStone_Project(WorkspaceParticipant wp, Project project);

    List<StoneParticipant> findAllByStoneAndWorkspaceParticipant_IsDeleteFalse(Stone stone);

    Optional<StoneParticipant> findByStone_IdAndWorkspaceParticipant_UserId(String stoneId, UUID userId);

    // 스톤 ID와 사용자 ID로 참여자 존재 여부 확인
    @Query("SELECT sp FROM StoneParticipant sp " +
            "WHERE sp.stone.id = :stoneId " +
            "AND sp.workspaceParticipant.userId = :userId " +
            "AND sp.isDelete = false")
    Optional<StoneParticipant> findByStoneIdAndUserId(
            @Param("stoneId") String stoneId,
            @Param("userId") UUID userId
    );
}
