package com.Dolmeng_E.workspace.domain.workspace.entity;

import com.Dolmeng_E.workspace.common.domain.BaseTimeEntity;
import com.Dolmeng_E.workspace.domain.access_group.entity.AccessGroup;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class WorkspaceParticipant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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



}
