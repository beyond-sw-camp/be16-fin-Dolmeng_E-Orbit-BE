package com.Dolmeng_E.workspace.domain.stone.entity;

import com.Dolmeng_E.workspace.domain.project.entity.Project;
import com.Dolmeng_E.workspace.domain.task.entity.Task;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import com.example.modulecommon.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@Setter
public class Stone extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stone_seq_generator")
    @GenericGenerator(
            name = "stone_seq_generator",
            strategy = "com.Dolmeng_E.workspace.common.domain.StringPrefixedSequenceIdGenerator",
            parameters = {
                    @org.hibernate.annotations.Parameter(name = "sequence_name", value = "stone_seq"),
                    @org.hibernate.annotations.Parameter(name = "initial_value", value = "1"),
                    @org.hibernate.annotations.Parameter(name = "increment_size", value = "1"),
                    @org.hibernate.annotations.Parameter(name = "valuePrefix", value = "pjt_s_")
            }
    )
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    // 스톤 담당자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stone_manager_id")
    private WorkspaceParticipant stoneManager;

    // 부모 스톤 (nullable 가능)
    @Column(name = "parent_stone_id")
    private String parentStoneId;

    // 스톤명
    @Column(name = "stone_name", nullable = false)
    private String stoneName;

    // 시작 시간
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    // 종료 시간
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    // 마일스톤 (진척도)
    @Column(name = "milestone", precision = 4, scale = 1, nullable = false)
    private BigDecimal milestone;

    // 채팅방 생성 여부
    @Column(name = "chat_creation", nullable = false)
    @Builder.Default
    private Boolean chatCreation = false;

    // 태스크 생성 여부
    @Column(name = "task_creation", nullable = false)
    @Builder.Default
    private Boolean taskCreation = true;

    // 진행 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StoneStatus status;

    // 태스크 수
    @Column(name = "task_count")
    @Builder.Default
    private Integer taskCount = 0;

    // 완료된 태스크 수
    @Column(name = "completed_count")
    @Builder.Default
    private Integer completedCount = 0;

    // 스톤 삭제 여부
    @Column(name = "is_delete", nullable = false)
    @Builder.Default
    private Boolean isDelete = false;

    // 추가 : 스톤 설명
    @Column(name = "stone_describe")
    private String stoneDescribe;

    // 자식스톤 관계 매핑을 위한 oneToMany(트리구조 조회시 활용)
    @OneToMany(mappedBy = "stone", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChildStoneList> childStoneLists = new ArrayList<>();

    @OneToMany(mappedBy = "stone", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<Task> tasks = new ArrayList<>();

    public void incrementTaskCount() {
        this.taskCount = (this.taskCount == null ? 1 : this.taskCount + 1);
    }

    public void decrementTaskCount() {
        if (this.taskCount == null || this.taskCount == 0) {
            this.taskCount = 0;
            return;
        }
        this.taskCount -= 1;
    }

    // 완료된 태스크 수 증가
    public void incrementCompletedCount() {
        this.completedCount = (this.completedCount == null ? 1 : this.completedCount + 1);
    }

    public void updateMilestone() {
        if (Boolean.TRUE.equals(this.isDelete)) {
            this.milestone = BigDecimal.ZERO;
            this.taskCount = 0;
            this.completedCount = 0;
            return;
        }

        // 1. 본인 태스크 수 계산
        int totalTasks = (tasks != null) ? tasks.size() : 0;
        int completedTasks = (tasks != null)
                ? (int) tasks.stream().filter(Task::getIsDone).count()
                : 0;

        // 2. 자식 스톤의 태스크도 누적
        for (ChildStoneList childRelation : childStoneLists) {
            Stone child = childRelation.getChildStone();
            if (!Boolean.TRUE.equals(child.getIsDelete())) {
                // 자식 먼저 최신화
                child.updateMilestone();

                // 자식의 태스크 수와 완료 수를 합산
                totalTasks += (child.getTaskCount() != null ? child.getTaskCount() : 0);
                completedTasks += (child.getCompletedCount() != null ? child.getCompletedCount() : 0);
            }
        }

        // 3. 태스크가 전혀 없으면 0%
        if (totalTasks == 0) {
            this.milestone = BigDecimal.ZERO;
            this.taskCount = 0;
            this.completedCount = 0;
            return;
        }

        // 4. 본인 + 자식 태스크 기준으로 계산
        BigDecimal ratio = BigDecimal.valueOf((double) completedTasks / totalTasks * 100)
                .setScale(1, RoundingMode.HALF_UP);

        this.milestone = ratio;
        this.taskCount = totalTasks;
        this.completedCount = completedTasks;
    }

}
