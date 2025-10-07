package com.Dolmeng_E.workspace.domain.workspace.entity;

import com.example.modulecommon.domain.BaseTimeEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class Workspace extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "workspace_generator")
    @GenericGenerator(
            name = "workspace_generator", // generator 이름
            strategy = "com.Dolmeng_E.workspace.common.domain.StringPrefixedSequenceIdGenerator", // 1단계에서 만든 클래스 경로
            parameters = {
                    @org.hibernate.annotations.Parameter(name = "sequence_name", value = "workspace_seq"), // DB에 생성할 시퀀스 이름
                    @org.hibernate.annotations.Parameter(name = "initial_value", value = "1"), // 시퀀스 시작 값
                    @org.hibernate.annotations.Parameter(name = "increment_size", value = "1"), // 시퀀스 증가 값
                    @Parameter(name = "valuePrefix", value = "ws_") // ID에 붙일 접두사!
            }
    )
    private String Id;

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
