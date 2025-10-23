package com.Dolmeng_E.workspace.domain.stone.entity;

import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import com.example.modulecommon.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class StoneParticipant extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "project_calendar_seq_generator")
    @GenericGenerator(
            name = "project_calendar_seq_generator",
            strategy = "com.Dolmeng_E.workspace.common.domain.StringPrefixedSequenceIdGenerator",
            parameters = {
                    @org.hibernate.annotations.Parameter(name = "sequence_name", value = "project_calendar_seq"),
                    @org.hibernate.annotations.Parameter(name = "initial_value", value = "1"),
                    @org.hibernate.annotations.Parameter(name = "increment_size", value = "1"),
                    @org.hibernate.annotations.Parameter(name = "valuePrefix", value = "pjt_s_par_")
            }
    )
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stone_id")
    private Stone stone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_participant_id")
    private WorkspaceParticipant workspaceParticipant;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isProjectHidden = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isMilestoneHidden = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDelete = false;

    public void updateMilestoneHidden(Boolean hidden) {
        this.isMilestoneHidden = hidden;
    }

    public void updateProjectHidden(Boolean hidden) {
        this.isProjectHidden = hidden;
    }

}
