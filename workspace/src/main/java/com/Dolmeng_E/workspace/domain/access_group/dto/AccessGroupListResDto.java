package com.Dolmeng_E.workspace.domain.access_group.dto;

import com.Dolmeng_E.workspace.domain.access_group.entity.AccessGroup;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class AccessGroupListResDto {
    private String accessGroupName;
    private LocalDateTime createdAt;
    private Integer groupParticipantCount; // 참여자 수

    public static AccessGroupListResDto fromEntity(AccessGroup accessGroup, Integer groupParticipantCount) {
        return AccessGroupListResDto.builder()
                .accessGroupName(accessGroup.getAccessGroupName())
                .createdAt(accessGroup.getCreatedAt())
                .groupParticipantCount(groupParticipantCount)
                .build();
    }
}
