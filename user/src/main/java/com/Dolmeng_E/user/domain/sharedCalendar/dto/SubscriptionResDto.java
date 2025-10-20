package com.Dolmeng_E.user.domain.sharedCalendar.dto;

import com.Dolmeng_E.user.domain.sharedCalendar.entity.CalendarSubscription;
import com.Dolmeng_E.user.domain.sharedCalendar.entity.SharedCalendar;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriptionResDto {
    private String id;
    private String workspaceId;
    private String subscriberUserId;
    private String targetUserId;
    private String sharedCalendarId;
    private String calendarName;

    // 구독 대상 유저의 공유 일정 리스트
    private List<SharedCalendarSubDto> sharedCalendars;

    public static SubscriptionResDto fromEntity(
            CalendarSubscription sub,
            List<SharedCalendarSubDto> sharedCalendars
    ) {
        return SubscriptionResDto.builder()
                .id(sub.getId())
                .workspaceId(sub.getWorkspaceId())
                .subscriberUserId(sub.getSubscriberUserId().getId().toString())
                .targetUserId(sub.getTargetUserId().getId().toString())
                .sharedCalendars(sharedCalendars)
                .build();
    }

    public static SubscriptionResDto fromEntity(CalendarSubscription sub) {
        return fromEntity(sub, null);
    }

    // 내부 DTO
    @Getter
    @Builder
    public static class SharedCalendarSubDto {
        private String calendarId;
        private String calendarName;
        private LocalDateTime startedAt;
        private LocalDateTime endedAt;

        public static SharedCalendarSubDto fromEntity(SharedCalendar calendar) {
            return SharedCalendarSubDto.builder()
                    .calendarId(calendar.getId())
                    .calendarName(calendar.getCalendarName())
                    .startedAt(calendar.getStartedAt())
                    .endedAt(calendar.getEndedAt())
                    .build();
        }
    }
}
