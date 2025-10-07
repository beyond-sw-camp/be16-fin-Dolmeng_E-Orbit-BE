package com.Dolmeng_E.workspace.domain.user_group.entity;

import com.Dolmeng_E.workspace.domain.workspace.entity.Workspace;
import com.example.modulecommon.domain.BaseTimeEntity;
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
public class UserGroup extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

    @NotNull
    @Column(length = 30)
    private String userGroupName;
}
