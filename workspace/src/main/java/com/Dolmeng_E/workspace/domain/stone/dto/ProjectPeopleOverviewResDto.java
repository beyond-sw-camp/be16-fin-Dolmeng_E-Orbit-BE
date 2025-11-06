package com.Dolmeng_E.workspace.domain.stone.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectPeopleOverviewResDto {
    private Integer totalPeopleCount;           // 총 인원
    private Integer managerCount;               // 적어도 1개 스톤을 '담당'하는 사람 수
    private Integer participantOnlyCount;       // 담당은 없고 참여만 있는 사람 수
    private List<ProjectMemberOverviewDto> people; // 카드용 목록
}