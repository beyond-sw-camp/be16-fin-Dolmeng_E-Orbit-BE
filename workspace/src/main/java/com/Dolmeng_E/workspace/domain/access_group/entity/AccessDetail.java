package com.Dolmeng_E.workspace.domain.access_group.entity;

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
public class AccessDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "access_detail_seq_generator")
    @GenericGenerator(
            name = "access_detail_seq_generator", // generator 이름
            strategy = "com.Dolmeng_E.workspace.common.domain.StringPrefixedSequenceIdGenerator", // 1단계에서 만든 클래스 경로
            parameters = {
                    @org.hibernate.annotations.Parameter(name = "sequence_name", value = "access_detail_seq"), // DB에 생성할 시퀀스 이름
                    @org.hibernate.annotations.Parameter(name = "initial_value", value = "1"), // 시퀀스 시작 값
                    @org.hibernate.annotations.Parameter(name = "increment_size", value = "1"), // 시퀀스 증가 값
                    @Parameter(name = "valuePrefix", value = "ws_acc_grp_detail") // ID에 붙일 접두사!
            }
    )
    private String Id;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    @JoinColumn(name = "access_group_id")
    private AccessGroup accessGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    @JoinColumn(name = "access_list_id")
    private AccessList accessList;

    @Builder.Default
    private Boolean isAccess = false;

}
