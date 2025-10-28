package com.Dolmeng_E.workspace.common.service;

import com.Dolmeng_E.workspace.domain.project.entity.Project;
import com.Dolmeng_E.workspace.domain.project.repository.ProjectRepository;
import com.Dolmeng_E.workspace.domain.stone.entity.Stone;
import com.Dolmeng_E.workspace.domain.stone.repository.StoneRepository;
import com.Dolmeng_E.workspace.domain.task.entity.Task;
import com.Dolmeng_E.workspace.domain.task.repository.TaskRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class MilestoneCalculator {

    private final TaskRepository taskRepository;
    private final StoneRepository stoneRepository;
    private final ProjectRepository projectRepository;

    // 스톤 및 상위스톤, 프로젝트까지 마일스톤 갱신
    public void updateStoneAndParents(Stone stone) {
        BigDecimal milestone = calculateStoneMilestone(stone);
        stone.setMilestone(milestone);
        stoneRepository.save(stone);

        if (stone.getParentStoneId() != null) {
            Stone parent = stoneRepository.findById(stone.getParentStoneId())
                    .orElseThrow(() -> new EntityNotFoundException("상위 스톤을 찾을 수 없습니다."));
            updateStoneAndParents(parent);
        } else {
            Project project = stone.getProject();
            project.setMilestone(stone.getMilestone());
            projectRepository.save(project);
        }
    }

    // 마일스톤 계산 로직
    public BigDecimal calculateStoneMilestone(Stone stone) {
        if (stone.getIsDelete() != null && stone.getIsDelete()) {
            return BigDecimal.ZERO;
        }

        List<Task> ownTasks = taskRepository.findAllByStone(stone);
        long totalTaskCount = ownTasks.size();
        long completedTaskCount = ownTasks.stream()
                .filter(Task::getIsDone)
                .count();

        List<Stone> childStones = stoneRepository.findAllByParentStoneIdAndIsDeleteFalse(stone.getId());

        // 자식스톤이 있는 경우: 자식스톤 마일스톤 평균 + 자신의 태스크 진척도 병합
        if (!childStones.isEmpty()) {
            BigDecimal totalMilestone = BigDecimal.ZERO;

            for (Stone child : childStones) {
                BigDecimal childMilestone = calculateStoneMilestone(child);
                totalMilestone = totalMilestone.add(childMilestone);
            }

            BigDecimal childAvg = totalMilestone
                    .divide(BigDecimal.valueOf(childStones.size()), 2, RoundingMode.HALF_UP);

            if (totalTaskCount > 0) {
                double ownRate = (double) completedTaskCount / totalTaskCount * 100;
                return childAvg.add(BigDecimal.valueOf(ownRate))
                        .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            } else {
                return childAvg;
            }
        }

        // 자식스톤이 없고 태스크도 없는 경우
        if (totalTaskCount == 0 && completedTaskCount == 0) {
            return BigDecimal.valueOf(100);
        }

        double ratio = (double) completedTaskCount / totalTaskCount * 100;
        return BigDecimal.valueOf(ratio).setScale(2, RoundingMode.HALF_UP);
    }
}
