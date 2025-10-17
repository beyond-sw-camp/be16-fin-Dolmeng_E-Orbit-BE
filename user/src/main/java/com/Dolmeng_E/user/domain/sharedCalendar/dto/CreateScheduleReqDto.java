package com.Dolmeng_E.user.domain.sharedCalendar.dto;

import com.Dolmeng_E.user.domain.sharedCalendar.entity.CalendarType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CreateScheduleReqDto {
    private String workspaceId;
    private String calendarName;
    private LocalDateTime startAt;
    private LocalDateTime endedAt;
    private Boolean isShared;
    private CalendarType CalendarType;
}