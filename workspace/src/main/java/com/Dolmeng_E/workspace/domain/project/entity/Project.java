package com.Dolmeng_E.workspace.domain.project.entity;

import com.Dolmeng_E.workspace.domain.project.dto.ProjectModifyDto;
import com.Dolmeng_E.workspace.domain.stone.entity.Stone;
import com.Dolmeng_E.workspace.domain.workspace.entity.Workspace;
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
public class Project extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "project_seq_generator")
    @GenericGenerator(
            name = "project_seq_generator",
            strategy = "com.Dolmeng_E.workspace.common.domain.StringPrefixedSequenceIdGenerator",
            parameters = {
                    @org.hibernate.annotations.Parameter(name = "sequence_name", value = "project_seq"),
                    @org.hibernate.annotations.Parameter(name = "initial_value", value = "1"),
                    @org.hibernate.annotations.Parameter(name = "increment_size", value = "1"),
                    @org.hibernate.annotations.Parameter(name = "valuePrefix", value = "pjt_")
            }
    )
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_participant_id", nullable = false)
    private WorkspaceParticipant workspaceParticipant;

    // fetch join용 양방향 리스트 추가
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Stone> stones = new ArrayList<>();

    @Column(name = "project_name", nullable = false)
    private String projectName;

    @Column(name = "project_objective")
    private String projectObjective;

    @Column(name = "project_description")
    private String projectDescription;

    @Column(precision = 4, scale = 1)
    private BigDecimal milestone;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_status", nullable = false)
    @Builder.Default
    private ProjectStatus projectStatus = ProjectStatus.PROGRESS; // PROGRESS,COMPLETED,STORAGE

    @Column(name = "is_delete", nullable = false)
    @Builder.Default
    private Boolean isDelete = false;

    // 전체 스톤 수
    @Column(name = "stone_count", nullable = false)
    @Builder.Default
    private Integer stoneCount = 0;

    // 완료된 스톤 수
    @Column(name = "completed_count", nullable = false)
    @Builder.Default
    private Integer completedCount = 0;

    public void update(ProjectModifyDto dto) {
        if (dto.getStartTime() != null) this.startTime = dto.getStartTime();
        if (dto.getEndTime() != null) this.endTime = dto.getEndTime();
        if (dto.getProjectObjective() != null) this.projectObjective = dto.getProjectObjective();
        if (dto.getProjectDescription() != null) this.projectDescription = dto.getProjectDescription();
        if (dto.getProjectStatus() != null) this.projectDescription = dto.getProjectDescription();
        if (dto.getProjectName() != null) this.projectName = dto.getProjectName();
        if (dto.getProjectStatus() != null) this.projectStatus = dto.getProjectStatus();
    }

    public void changeManager(WorkspaceParticipant newManager) {
        this.workspaceParticipant = newManager;
    }

    public void deleteProject() {
        this.isDelete = true;
    }

    public void updateMilestone() {
        if (stoneCount == null || stoneCount == 0) {
            this.milestone = BigDecimal.ZERO;
            return;
        }

        BigDecimal completed = BigDecimal.valueOf(completedCount == null ? 0 : completedCount);
        BigDecimal total = BigDecimal.valueOf(stoneCount);

        // (완료 / 전체) * 100 → 소수점 1자리 반올림
        this.milestone = completed
                .divide(total, 3, RoundingMode.HALF_UP) // 내부 계산 정밀도 확보용 3자리
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
    }

    public void incrementStoneCount() {
        if (stoneCount == null) stoneCount = 0;
        stoneCount++;
        updateMilestone();
    }

    public void decrementStoneCount() {
        if (stoneCount == null) stoneCount = 0;
        stoneCount = Math.max(0, stoneCount - 1);
        updateMilestone();
    }

    public void incrementCompletedCount() {
        if (completedCount == null) completedCount = 0;
        completedCount++;
        updateMilestone();
    }

    public void decrementCompletedCount() {
        if (completedCount == null) completedCount = 0;
        completedCount = Math.max(0, completedCount - 1);
        updateMilestone();
    }


}
