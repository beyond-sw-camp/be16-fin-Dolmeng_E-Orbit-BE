package com.Dolmeng_E.user.domain.sharedCalendar.dto;

import com.Dolmeng_E.user.domain.sharedCalendar.entity.SharedCalendar;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class TodoCreateResDto {
    private String id;
    private String userId;
    private String workspaceId;
    private String calendarName;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Boolean bookmark;
    private Boolean isCompleted;

    public static TodoCreateResDto fromEntity(SharedCalendar cal) {
        return TodoCreateResDto.builder()
                .id(cal.getId())
                .userId(cal.getUserId().getId().toString())
                .workspaceId(cal.getWorkspaceId())
                .calendarName(cal.getCalendarName())
                .startedAt(cal.getStartedAt())
                .endedAt(cal.getEndedAt())
                .bookmark(cal.getBookmark())
                .isCompleted(cal.getIsCompleted())
                .build();
    }
}
