package com.Dolmeng_E.workspace.domain.access_group.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class AccessDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "access_group_id")
    private Long accessGroupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "access_list_id")
    private Long accessListId;

    @Builder.Default
    private Boolean isAccess = false;

}
