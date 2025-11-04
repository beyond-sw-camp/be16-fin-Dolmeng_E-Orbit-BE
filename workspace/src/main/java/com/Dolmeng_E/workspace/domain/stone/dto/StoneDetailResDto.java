package com.Dolmeng_E.workspace.domain.stone.dto;

import com.Dolmeng_E.workspace.domain.project.entity.Project;
import com.Dolmeng_E.workspace.domain.stone.entity.Stone;
import com.Dolmeng_E.workspace.domain.stone.entity.StoneStatus;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class StoneDetailResDto {

    // 스톤 담당자 이름
    private String stoneManagerName;

    // 스톤명
    private String stoneName;

    // 시작 시간
    private LocalDateTime startTime;

    // 종료 시간
    private LocalDateTime endTime;

    // 채팅방 생성 여부
    private Boolean chatCreation;

    // 태스크 목록
    private List<TaskResDto> taskResDtoList;

    // 참여자 목록
    private List<StoneParticipantDto> stoneParticipantDtoList;

    // +추가 : 스톤 상태
    private StoneStatus stoneStatus;

    // 추가 : 스톤 설명
    private String stoneDescribe;

    // 추가 : 마일스톤
    private BigDecimal milestone;

    public static StoneDetailResDto fromEntity(Stone stone, List<TaskResDto> taskResDtoList, List<StoneParticipantDto> stoneParticipantDtoList) {
        return StoneDetailResDto.builder()
                .stoneManagerName(stone.getStoneManager().getUserName())
                .stoneName(stone.getStoneName())
                .startTime(stone.getStartTime())
                .endTime(stone.getEndTime())
                .chatCreation(stone.getChatCreation())
                .taskResDtoList(taskResDtoList)
                .stoneParticipantDtoList(stoneParticipantDtoList)
                .stoneStatus(stone.getStatus()) // 추가
                .stoneDescribe(stone.getStoneDescribe()) // 추가
                .milestone(stone.getMilestone()) // 추가
                .build();
    }

}
