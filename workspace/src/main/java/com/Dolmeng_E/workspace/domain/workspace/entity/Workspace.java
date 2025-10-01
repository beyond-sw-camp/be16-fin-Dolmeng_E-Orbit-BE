package com.Dolmeng_E.workspace.domain.workspace.entity;

import com.Dolmeng_E.workspace.common.domain.BaseTimeEntity;
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
public class Workspace extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    private WorkspaceTemplates workspaceTemplates;

    @NotNull
    @Column(length = 20)
    private String workspaceName;

//    결제 구현 완료 시 구독개월 활용 예정
    private Integer subscribe;

    @Builder.Default
    private Long currentStorage = 0L;

    private Long maxStorage;

//    워크스페이스 생성자 ID
    @NotNull
    private UUID userId;

    public void settingMaxStorage(WorkspaceTemplates workspaceTemplates) {
        switch (workspaceTemplates) {
            case PERSONAL -> this.maxStorage = 10L * 1024 * 1024 * 1024;       // 10GB
            case PRO -> this.maxStorage = 50L * 1024 * 1024 * 1024;            // 50GB
            case ENTERPRISE -> this.maxStorage = 100L * 1024 * 1024 * 1024;    // 100GB
        }
    }

}
