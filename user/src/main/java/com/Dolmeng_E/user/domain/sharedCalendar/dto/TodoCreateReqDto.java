package com.Dolmeng_E.user.domain.sharedCalendar.dto;

import com.Dolmeng_E.user.domain.sharedCalendar.entity.CalendarType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TodoCreateReqDto {
    private String workspaceId;
    @Builder.Default
    private CalendarType calendarType = CalendarType.TODO;
    private String calendarName;
    // Todo: 일자 + 시간 설정 어떻게 구현할 것인지 생각해보기
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Boolean bookmark;
}
