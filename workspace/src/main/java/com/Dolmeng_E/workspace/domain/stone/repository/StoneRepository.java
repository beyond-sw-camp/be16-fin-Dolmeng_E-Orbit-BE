package com.Dolmeng_E.workspace.domain.stone.repository;

import com.Dolmeng_E.workspace.domain.project.entity.Project;
import com.Dolmeng_E.workspace.domain.stone.entity.Stone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StoneRepository extends JpaRepository<Stone, String> {
    List<Stone> findAllByProject(Project project);

    // 특정 프로젝트 내 스톤 전체 조회 (삭제된 것 제외)
    List<Stone> findByProjectAndIsDeleteFalse(Project project);
}
