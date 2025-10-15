package com.Dolmeng_E.workspace.domain.stone.entity;

import com.Dolmeng_E.workspace.domain.project.entity.Project;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import com.example.modulecommon.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    @Column(name = "milestone", precision = 3, scale = 1, nullable = false)
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
    private Integer taskCount;

    // 완료된 태스크 수
    @Column(name = "completed_count")
    private Integer completedCount;

    // 스톤 삭제 여부
    @Column(name = "is_delete", nullable = false)
    @Builder.Default
    private Boolean isDelete = false;

}
