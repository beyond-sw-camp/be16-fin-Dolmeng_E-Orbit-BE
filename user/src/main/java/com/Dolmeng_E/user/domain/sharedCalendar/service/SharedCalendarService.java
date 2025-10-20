package com.Dolmeng_E.user.domain.sharedCalendar.service;

import com.Dolmeng_E.user.domain.sharedCalendar.dto.CreateScheduleReqDto;
import com.Dolmeng_E.user.domain.sharedCalendar.dto.SharedCalendarResDto;
import com.Dolmeng_E.user.domain.sharedCalendar.dto.UpdateScheduleReqDto;
import com.Dolmeng_E.user.domain.sharedCalendar.entity.SharedCalendar;
import com.Dolmeng_E.user.domain.sharedCalendar.repository.SharedCalendarRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SharedCalendarService {

    private final SharedCalendarRepository sharedCalendarRepository;
    private final CalendarValidationService validationService;

    // 일정 등록
    public SharedCalendarResDto createSchedule(UUID userId, CreateScheduleReqDto dto) {

        // 1. 검증
        var user = validationService.validateUserAndWorkspace(userId, dto.getWorkspaceId());

        // 2. 일정 생성
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

    // 일정 조회
    public List<SharedCalendarResDto> getSchedules(UUID userId, String workspaceId) {
        // 1. 검증
        validationService.validateUserAndWorkspace(userId, workspaceId);

        // 2. 일정 조회
        List<SharedCalendar> calendars = sharedCalendarRepository.findByUserIdAndWorkspaceId(userId, workspaceId);
        return calendars.stream()
                .map(SharedCalendarResDto::fromEntity)
                .toList();
    }

    // 일정 수정
    public SharedCalendarResDto updateSchedule(String calendarId, UUID userId, UpdateScheduleReqDto dto) {
        // 1. 검증
        var calendar = sharedCalendarRepository.findById(calendarId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 일정입니다."));

        validationService.validateUserAndWorkspace(userId, calendar.getWorkspaceId());

        if (!calendar.getUserId().getId().equals(userId))
            throw new IllegalArgumentException("본인 일정만 수정할 수 있습니다.");

        // 2. 일정 수정
        calendar.update(dto.getCalendarName(), dto.getStartAt(), dto.getEndedAt(), dto.getIsShared());
        return SharedCalendarResDto.fromEntity(calendar);
    }

    // 일정 삭제
    public void deleteSchedule(String calendarId, UUID userId) {
        // 1. 검증
        var calendar = sharedCalendarRepository.findById(calendarId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 일정입니다."));

        validationService.validateUserAndWorkspace(userId, calendar.getWorkspaceId());

        if (!calendar.getUserId().getId().equals(userId))
            throw new IllegalArgumentException("본인 일정만 삭제할 수 있습니다.");

        // 2. 일정 삭제
        sharedCalendarRepository.delete(calendar);
    }
}