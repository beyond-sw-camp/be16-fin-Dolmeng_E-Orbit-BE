package com.Dolmeng_E.user.domain.sharedCalendar.dto;

import com.Dolmeng_E.user.domain.sharedCalendar.entity.SharedCalendar;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SharedCalendarResDto {
    private String id;
    private String userId;
    private String workspaceId;
    private String calendarName;
    private LocalDateTime startAt;
    private LocalDateTime endedAt;
    private Boolean isShared;

    public static SharedCalendarResDto fromEntity(SharedCalendar cal) {
        return SharedCalendarResDto.builder()
                .id(cal.getId())
                .userId(cal.getUserId().getId().toString())
                .workspaceId(cal.getWorkspaceId())
                .calendarName(cal.getCalendarName())
                .startAt(cal.getStartAt())
                .endedAt(cal.getEndedAt())
                .isShared(cal.getIsShared())
                .build();
    }
}
