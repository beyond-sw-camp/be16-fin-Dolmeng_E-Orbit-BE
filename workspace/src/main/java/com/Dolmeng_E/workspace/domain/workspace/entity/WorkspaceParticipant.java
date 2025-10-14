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
public class WorkspaceParticipant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "workspaceParticipant_generator")
    @GenericGenerator(
            name = "workspaceParticipant_generator", // generator 이름
            strategy = "com.Dolmeng_E.workspace.common.domain.StringPrefixedSequenceIdGenerator", // 1단계에서 만든 클래스 경로
            parameters = {
                    @org.hibernate.annotations.Parameter(name = "sequence_name", value = "workspaceParticipant_seq"), // DB에 생성할 시퀀스 이름
                    @org.hibernate.annotations.Parameter(name = "initial_value", value = "1"), // 시퀀스 시작 값
                    @org.hibernate.annotations.Parameter(name = "increment_size", value = "1"), // 시퀀스 증가 값
                    @Parameter(name = "valuePrefix", value = "ws_pt_") // ID에 붙일 접두사!
            }
    )
    private String Id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    @NotNull
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "access_group_id")
    @NotNull
    private AccessGroup accessGroup;

    @NotNull
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


}
