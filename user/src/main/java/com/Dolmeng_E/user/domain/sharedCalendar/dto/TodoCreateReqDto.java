package com.Dolmeng_E.user.domain.sharedCalendar.dto;

import com.Dolmeng_E.user.domain.sharedCalendar.entity.CalendarType;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;

@Getter
@Builder
public class TodoCreateReqDto {
    private String workspaceId;
    @Builder.Default
    private CalendarType calendarType = CalendarType.TODO;
    private String calendarName;
    private LocalDate date;
    private Boolean bookmark;
}
