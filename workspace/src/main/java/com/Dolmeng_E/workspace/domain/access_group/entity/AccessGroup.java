package com.Dolmeng_E.workspace.domain.access_group.entity;

import com.Dolmeng_E.workspace.common.domain.BaseTimeEntity;
import com.Dolmeng_E.workspace.domain.workspace.entity.Workspace;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class AccessGroup extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

    @NotNull
    @Column(length = 10)
    private String accessGroupName;
}
