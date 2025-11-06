package com.Dolmeng_E.user.domain.sharedCalendar.dto;

import lombok.Getter;
import java.time.LocalDate;

@Getter
public class UpdateTodoReqDto {
    private String calendarName;
    private LocalDate date;
    private Boolean bookmark;
}
