package com.Dolmeng_E.workspace.domain.access_group.entity;

import com.Dolmeng_E.workspace.domain.workspace.entity.Workspace;
import com.example.modulecommon.domain.BaseTimeEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@Table(
        name = "access_group",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"workspace_id", "access_group_name"}
                )
        }
)
public class AccessGroup extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "access_group_seq_generator")
    @GenericGenerator(
            name = "access_group_seq_generator",
            strategy = "com.Dolmeng_E.workspace.common.domain.StringPrefixedSequenceIdGenerator",
            parameters = {
                    @org.hibernate.annotations.Parameter(name = "sequence_name", value = "access_group_seq"),
                    @org.hibernate.annotations.Parameter(name = "initial_value", value = "1"),
                    @org.hibernate.annotations.Parameter(name = "increment_size", value = "1"),
                    @Parameter(name = "valuePrefix", value = "ws_acc_grp_")
            }
    )
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @NotNull
    @Column(name = "access_group_name", length = 10)
    private String accessGroupName;

    public void setAccessGroupName(String accessGroupName) {
        if (!accessGroupName.equals(this.accessGroupName)) {
            this.accessGroupName = accessGroupName;
        }
    }
}