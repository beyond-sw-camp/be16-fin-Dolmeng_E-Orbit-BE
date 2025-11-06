package com.Dolmeng_E.workspace.domain.stone.entity;

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
public class ChildStoneList {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stone_ch_list_seq_generator")
    @GenericGenerator(
            name = "stone_ch_list_seq_generator",
            strategy = "com.Dolmeng_E.workspace.common.domain.StringPrefixedSequenceIdGenerator",
            parameters = {
                    @org.hibernate.annotations.Parameter(name = "sequence_name", value = "stone_ch_list_seq"),
                    @org.hibernate.annotations.Parameter(name = "initial_value", value = "1"),
                    @org.hibernate.annotations.Parameter(name = "increment_size", value = "1"),
                    @org.hibernate.annotations.Parameter(name = "valuePrefix", value = "pjt_s_ch_")
            }
    )
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stone_id")
    private Stone stone; // 부모스톤 ex) pjt_s_1

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_stone_id")
    private Stone childStone; // 자식스톤들 ex) pjt_s_2, pjt_s_3 ...
}
