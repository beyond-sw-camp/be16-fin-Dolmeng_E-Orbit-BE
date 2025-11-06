package com.Dolmeng_E.workspace.domain.workspace.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserGroupProjectProgressResDto {

    private String groupName;

    private int projectCount;

    private double averageProgress;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;
}
