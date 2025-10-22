package com.Dolmeng_E.user.domain.sharedCalendar.dto;

import com.Dolmeng_E.user.domain.sharedCalendar.entity.RepeatCycle;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UpdateScheduleReqDto {
    private String calendarName;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Boolean isShared;
    private RepeatCycle repeatCycle;        // NONE, DAILY, WEEKLY, MONTHLY, YEARLY
    private LocalDateTime repeatEndAt;      // 반복 종료일
    private Boolean applyToGroup;           // true면 반복 그룹 전체 수정
}