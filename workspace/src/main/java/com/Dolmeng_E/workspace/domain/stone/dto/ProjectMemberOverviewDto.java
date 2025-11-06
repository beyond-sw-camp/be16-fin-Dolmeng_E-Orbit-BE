package com.Dolmeng_E.workspace.domain.stone.dto;

import com.Dolmeng_E.workspace.common.dto.UserInfoResDto;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMemberOverviewDto {
    private UserInfoResDto user;              // 사용자 프로필(이름/메일/아바타 등)

    private Integer ownedStoneCount;          // 담당 스톤 개수
    private Integer participatingStoneCount;  // 참여 스톤 개수(담당 제외 또는 포함, 아래 로직 기준)

    private List<SimpleStoneRefDto> ownedStones;         // 담당 스톤 목록(간단 표기)
    private List<SimpleStoneRefDto> participatingStones; // 참여 스톤 목록(간단 표기)

    private Integer myTaskTotal;              // 내가 맡은 태스크 총 수 (task.taskManager == me)
    private Integer myTaskCompleted;          // 그 중 완료된 수
}