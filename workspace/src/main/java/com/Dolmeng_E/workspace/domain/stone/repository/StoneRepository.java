package com.Dolmeng_E.workspace.domain.stone.repository;

import com.Dolmeng_E.workspace.domain.project.entity.Project;
import com.Dolmeng_E.workspace.domain.stone.entity.Stone;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StoneRepository extends JpaRepository<Stone, String> {
    List<Stone> findAllByProject(Project project);

    // 특정 프로젝트 내 스톤 전체 조회 (삭제된 것 제외)
    List<Stone> findByProjectAndIsDeleteFalse(Project project);

    List<Stone> findAllByParentStoneIdAndIsDeleteFalse(String stoneId);

    @Query("SELECT s FROM Stone s WHERE s.project.id = :projectId AND s.isDelete = false")
    List<Stone> findAllByProjectId(@Param("projectId") String projectId);

    // 워크스페이스 ID 기준 스톤 전체 조회
    @Query("""
    select s
    from Stone s
    join s.project p
    where p.workspace.id = :workspaceId
    and s.isDelete = false
    """)
    List<Stone> findAllByWorkspaceId(@Param("workspaceId") String workspaceId);

    // 1) 루트 제외 + 삭제 제외 전체 스톤 수
    @Query("""
           select count(s)
           from Stone s
           where s.project.id = :projectId
             and s.isDelete = false
             and s.parentStoneId is not null
           """)
    long countActiveNonRootByProjectId(@Param("projectId") String projectId);

    // 2) 루트 제외 + 삭제 제외 + 완료 상태 스톤 수
    @Query("""
           select count(s)
           from Stone s
           where s.project.id = :projectId
             and s.isDelete = false
             and s.parentStoneId is not null
             and s.status = com.Dolmeng_E.workspace.domain.stone.entity.StoneStatus.COMPLETED
           """)
    long countCompletedNonRootByProjectId(@Param("projectId") String projectId);

}
