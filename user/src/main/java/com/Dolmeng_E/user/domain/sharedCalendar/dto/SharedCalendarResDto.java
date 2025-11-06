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
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Boolean isShared;
    private String userName;

    public static SharedCalendarResDto fromEntity(SharedCalendar cal) {
        return SharedCalendarResDto.builder()
                .id(cal.getId())
                .userId(cal.getUserId().getId().toString())
                .workspaceId(cal.getWorkspaceId())
                .calendarName(cal.getCalendarName())
                .startedAt(cal.getStartedAt())
                .endedAt(cal.getEndedAt())
                .isShared(cal.getIsShared())
                .userName(cal.getUserId().getName())
                .build();
    }
}
