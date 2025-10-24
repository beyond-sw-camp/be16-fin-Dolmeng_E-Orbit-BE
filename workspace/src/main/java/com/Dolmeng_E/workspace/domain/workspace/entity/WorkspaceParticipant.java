package com.Dolmeng_E.workspace.domain.workspace.entity;

import com.Dolmeng_E.workspace.domain.access_group.entity.AccessGroup;
import com.example.modulecommon.domain.BaseTimeEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@Setter
@Table(
        name = "workspace_participant",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"workspace_id", "user_id"})
        }
)
public class WorkspaceParticipant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "workspaceParticipant_generator")
    @GenericGenerator(
            name = "workspaceParticipant_generator",
            strategy = "com.Dolmeng_E.workspace.common.domain.StringPrefixedSequenceIdGenerator",
            parameters = {
                    @Parameter(name = "sequence_name", value = "workspaceParticipant_seq"),
                    @Parameter(name = "initial_value", value = "1"),
                    @Parameter(name = "increment_size", value = "1"),
                    @Parameter(name = "valuePrefix", value = "ws_pt_")
            }
    )
    private String Id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    @NotNull
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "access_group_id")
    private AccessGroup accessGroup;

    @NotNull
    @Column(name = "user_id")
    private UUID userId;

    @NotNull
    private String userName;

    @NotNull
    @Enumerated(EnumType.STRING)
    private WorkspaceRole workspaceRole;

    @NotNull
    @Builder.Default
    private Boolean isDelete = false;

    public boolean isDelete() {
        return isDelete;
    }

    public void deleteParticipant() {
        this.isDelete = true;
    }

    public void restoreParticipant() {
        this.isDelete = false;
    }


}
