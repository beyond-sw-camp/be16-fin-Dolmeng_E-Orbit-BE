package com.Dolmeng_E.user.domain.sharedCalendar.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UpdateScheduleReqDto {
    private String calendarName;
    private LocalDateTime startAt;
    private LocalDateTime endedAt;
    private Boolean isShared;
}