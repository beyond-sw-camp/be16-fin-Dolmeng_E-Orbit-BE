package com.Dolmeng_E.user.domain.sharedCalendar.service;

import com.Dolmeng_E.user.common.service.WorkspaceFeign;
import com.Dolmeng_E.user.domain.sharedCalendar.dto.CreateScheduleReqDto;
import com.Dolmeng_E.user.domain.sharedCalendar.dto.SharedCalendarResDto;
import com.Dolmeng_E.user.domain.sharedCalendar.dto.UpdateScheduleReqDto;
import com.Dolmeng_E.user.domain.sharedCalendar.entity.SharedCalendar;
import com.Dolmeng_E.user.domain.sharedCalendar.repository.CalendarSubscriptionRepository;
import com.Dolmeng_E.user.domain.sharedCalendar.repository.SharedCalendarRepository;
import com.Dolmeng_E.user.domain.user.entity.User;
import com.Dolmeng_E.user.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SharedCalendarService {

    private final SharedCalendarRepository sharedCalendarRepository;
    private final CalendarSubscriptionRepository calendarSubscriptionRepository;
    private final UserRepository userRepository;
    private final WorkspaceFeign workspaceFeign;

    // 일정 등록
    public SharedCalendarResDto createSchedule(UUID userId, CreateScheduleReqDto dto) {

        // 1. 유저 존재 여부
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        // 2. 워크스페이스 존재 여부 검증
        if (!workspaceFeign.checkWorkspaceExists(dto.getWorkspaceId())) {
            throw new IllegalArgumentException("존재하지 않는 워크스페이스입니다.");
        }

        // 3. 유저 워크스페이스 참여 여부 검증
        if (!workspaceFeign.checkWorkspaceMembership(dto.getWorkspaceId(), userId)) {
            throw new IllegalArgumentException("해당 유저는 워크스페이스에 속하지 않습니다.");
        }

        // 4. 검증 통과 시 일정 생성
        SharedCalendar calendar = SharedCalendar.builder()
                .userId(user)
                .workspaceId(dto.getWorkspaceId())
                .calendarName(dto.getCalendarName())
                .startAt(dto.getStartAt())
                .endedAt(dto.getEndedAt())
                .isShared(dto.getIsShared())
                .build();

        sharedCalendarRepository.save(calendar);
        return SharedCalendarResDto.fromEntity(calendar);
    }

    // 일정 조회 (본인 + 구독 유저들의 공유 일정)
    public List<SharedCalendarResDto> getSchedules(UUID userId) {
        List<UUID> subscribedUserIds = calendarSubscriptionRepository.findTargetUserIdsBySubscriber(userId);

        // 본인 일정 + 구독 유저 공유 일정
        List<SharedCalendar> allCalendars = new ArrayList<>();
        allCalendars.addAll(sharedCalendarRepository.findByUserIdOrShared(userId));
        if (!subscribedUserIds.isEmpty()) {
            allCalendars.addAll(sharedCalendarRepository.findSharedCalendarsOfSubscribedUsers(subscribedUserIds));
        }

        return allCalendars.stream()
                .map(SharedCalendarResDto::fromEntity)
                .distinct()
                .collect(Collectors.toList());
    }

    // 일정 수정
    public SharedCalendarResDto updateSchedule(String calendarId, UUID userId, UpdateScheduleReqDto dto) {
        SharedCalendar calendar = sharedCalendarRepository.findById(calendarId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 일정입니다."));

        if (!calendar.getUserId().getId().equals(userId))
            throw new IllegalArgumentException("본인 일정만 수정할 수 있습니다.");

        calendar.update(dto.getCalendarName(), dto.getStartAt(), dto.getEndedAt(), dto.getIsShared());
        return SharedCalendarResDto.fromEntity(calendar);
    }

    // 일정 삭제
    public void deleteSchedule(String calendarId, UUID userId) {
        SharedCalendar calendar = sharedCalendarRepository.findById(calendarId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 일정입니다."));

        if (!calendar.getUserId().getId().equals(userId))
            throw new IllegalArgumentException("본인 일정만 삭제할 수 있습니다.");

        sharedCalendarRepository.delete(calendar);
    }
}