package com.Dolmeng_E.workspace.domain.task.entity;

import com.Dolmeng_E.workspace.domain.stone.entity.Stone;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import com.example.modulecommon.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@Setter
public class Task extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_seq_generator")
    @GenericGenerator(
            name = "task_seq_generator",
            strategy = "com.Dolmeng_E.workspace.common.domain.StringPrefixedSequenceIdGenerator",
            parameters = {
                    @org.hibernate.annotations.Parameter(name = "sequence_name", value = "task_seq"),
                    @org.hibernate.annotations.Parameter(name = "initial_value", value = "1"),
                    @org.hibernate.annotations.Parameter(name = "increment_size", value = "1"),
                    @org.hibernate.annotations.Parameter(name = "valuePrefix", value = "pjt_s_task_")
            }
    )
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stone_id")
    private Stone stone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_manager_id")
    private WorkspaceParticipant taskManager;

    @Column(name = "task_name", nullable = false, length = 255)
    private String taskName;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "is_done", nullable = false)
    @Builder.Default
    private Boolean isDone = false;

    // 추가 : 태스크 완료 날짜 -> 태스크 완료처리 시 현재 날짜를 추가
    @Column(name = "task_completed_date")
    private LocalDateTime taskCompletedDate;

    // 추가 : 태스크 reopened count -> 태스크 취소에서 완료로 상태변경 시 1 추가
    @Column(name = "reopened_count")
    private Integer reopenedCount;

    // 추가 : 지연된 task 수 -> 조회시 완료날짜보다 오늘 날짜가 지나갔으면 true
    @Column(name = "is_delayed_task")
    private boolean isDelayedTask;


}
