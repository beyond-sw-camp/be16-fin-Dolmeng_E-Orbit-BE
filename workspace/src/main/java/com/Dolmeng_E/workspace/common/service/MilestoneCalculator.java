package com.Dolmeng_E.workspace.common.service;

import com.Dolmeng_E.workspace.domain.project.entity.Project;
import com.Dolmeng_E.workspace.domain.project.repository.ProjectRepository;
import com.Dolmeng_E.workspace.domain.stone.entity.Stone;
import com.Dolmeng_E.workspace.domain.stone.repository.StoneRepository;
import com.Dolmeng_E.workspace.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MilestoneCalculator {

    private final StoneRepository stoneRepository;
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    /**
     * 태스크 완료/생성 등 상태 변경 시
     * 현재 스톤 기준으로 상위 스톤과 프로젝트까지 마일스톤 전파
     */
    public void updateStoneAndParents(Stone stone) {
        if (stone == null || Boolean.TRUE.equals(stone.getIsDelete())) return;

        log.info("🟡 [START] 스톤({}) 마일스톤 계산 시작 (parent={})",
                stone.getStoneName(), stone.getParentStoneId());

        // 1. 본인 및 하위 스톤 태스크 기반으로 milestone 계산
        updateStoneMilestone(stone);

        // 2. 부모가 있으면 상향 전파
        if (stone.getParentStoneId() != null) {
            Stone parent = stoneRepository.findById(stone.getParentStoneId()).orElse(null);
            if (parent != null) {
                log.info(" [PARENT] 상위 스톤({}) 마일스톤 갱신", parent.getStoneName());
                updateStoneAndParents(parent);
                return;
            }
        }

        // 3. 루트면 프로젝트 마일스톤 업데이트
        if (stone.getParentStoneId() == null) {
            Project project = stone.getProject();
            BigDecimal rootMilestone = stone.getMilestone();
            project.setMilestone(rootMilestone);
            projectRepository.saveAndFlush(project);
            log.info(" [PROJECT] 프로젝트({}) 마일스톤 = {}%", project.getProjectName(), rootMilestone);
        }

        log.info(" [DONE] 스톤({}) milestone={}%, total={}, done={}",
                stone.getStoneName(), stone.getMilestone(), stone.getTaskCount(), stone.getCompletedCount());
    }

    /**
     * 특정 스톤의 milestone 계산 (본인 + 모든 하위 스톤의 태스크 기준)
     */
    private void updateStoneMilestone(Stone stone) {
        long total = 0;
        long done = 0;

        // 본인 태스크 수
        long ownTotal = taskRepository.countByStone(stone);
        long ownDone = taskRepository.countByStoneAndIsDoneTrue(stone);
        total += ownTotal;
        done += ownDone;

        // 하위 스톤(1 depth) 태스크 합산
        List<Stone> children = stoneRepository.findAllByParentStoneIdAndIsDeleteFalse(stone.getId());
        for (Stone child : children) {
            total += child.getTaskCount() != null ? child.getTaskCount() : 0;
            done += child.getCompletedCount() != null ? child.getCompletedCount() : 0;
        }

        // 계산
        BigDecimal milestone;
        if (total == 0) {
            milestone = BigDecimal.ZERO;
            stone.setTaskCount(0);
            stone.setCompletedCount(0);
        } else {
            milestone = BigDecimal.valueOf((done * 100.0) / total)
                    .setScale(1, RoundingMode.HALF_UP);
            stone.setTaskCount((int) total);
            stone.setCompletedCount((int) done);
        }

        stone.setMilestone(milestone);
        stoneRepository.saveAndFlush(stone);

        log.debug(" [STONE] {} → total={}, done={}, milestone={}%",
                stone.getStoneName(), total, done, milestone);
    }
}
