package com.Dolmeng_E.user.domain.sharedCalendar.dto;

import com.Dolmeng_E.user.domain.sharedCalendar.entity.CalendarType;
import com.Dolmeng_E.user.domain.sharedCalendar.entity.RepeatCycle;
import lombok.Builder;
import lombok.Getter;
import com.Dolmeng_E.user.domain.sharedCalendar.entity.CalendarType;


import java.time.LocalDateTime;

@Getter
@Builder
public class CreateScheduleReqDto {
    private String workspaceId;
    private String calendarName;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Boolean isShared;
    private RepeatCycle repeatCycle;        // NONE, DAILY, WEEKLY, MONTHLY, YEARLY
    private LocalDateTime repeatEndAt;      // 반복 종료일
    @Builder.Default
    private CalendarType calendarType = CalendarType.SCHEDULE;
}
