package com.Dolmeng_E.user.domain.sharedCalendar.service;

import com.Dolmeng_E.user.domain.sharedCalendar.dto.CreateScheduleReqDto;
import com.Dolmeng_E.user.domain.sharedCalendar.dto.SharedCalendarResDto;
import com.Dolmeng_E.user.domain.sharedCalendar.dto.UpdateScheduleReqDto;
import com.Dolmeng_E.user.domain.sharedCalendar.entity.CalendarType;
import com.Dolmeng_E.user.domain.sharedCalendar.entity.RepeatCycle;
import com.Dolmeng_E.user.domain.sharedCalendar.entity.SharedCalendar;
import com.Dolmeng_E.user.domain.sharedCalendar.repository.SharedCalendarRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SharedCalendarService {

    private final SharedCalendarRepository sharedCalendarRepository;
    private final CalendarValidationService validationService;

    // 일정 등록
    public List<SharedCalendarResDto> createSchedule(UUID userId, CreateScheduleReqDto dto) {

        // 1. 검증
        var user = validationService.validateUserAndWorkspace(userId, dto.getWorkspaceId());

        CalendarType type = dto.getCalendarType() != null
                ? dto.getCalendarType()
                : CalendarType.SCHEDULE;

        RepeatCycle repeatCycle = dto.getRepeatCycle() != null
                ? dto.getRepeatCycle()
                : RepeatCycle.NONE;

        // 2. 반복 일정 생성
        List<SharedCalendar> schedules = new ArrayList<>();

        LocalDateTime start = dto.getStartedAt();
        LocalDateTime end = dto.getEndedAt();
        LocalDateTime repeatEnd = dto.getRepeatEndAt();
        String repeatGroupId = UUID.randomUUID().toString();

        // 일정 반복 없을 경우, 한 번만 저장
        if (repeatCycle == RepeatCycle.NONE || repeatEnd == null) {
            SharedCalendar calendar = SharedCalendar.builder()
                    .userId(user)
                    .workspaceId(dto.getWorkspaceId())
                    .calendarType(type)
                    .calendarName(dto.getCalendarName())
                    .startedAt(start)
                    .endedAt(end)
                    .repeatCycle(RepeatCycle.NONE)
                    .isShared(dto.getIsShared())
                    .repeatGroupId(repeatGroupId)
                    .build();
            sharedCalendarRepository.save(calendar);
            return List.of(SharedCalendarResDto.fromEntity(calendar));
        }

        // 일정 반복이 있을 경우
        while (!start.isAfter(repeatEnd)) {
            SharedCalendar calendar = SharedCalendar.builder()
                    .userId(user)
                    .workspaceId(dto.getWorkspaceId())
                    .calendarType(type)
                    .calendarName(dto.getCalendarName())
                    .startedAt(start)
                    .endedAt(end)
                    .repeatCycle(repeatCycle)
                    .repeatEndAt(repeatEnd)
                    .isShared(dto.getIsShared())
                    .repeatGroupId(repeatGroupId)
                    .build();
            schedules.add(calendar);

            // 다음 일정 계산
            switch (repeatCycle) {
                case DAILY -> {
                    start = start.plusDays(1);
                    end = end.plusDays(1);
                }
                case WEEKLY -> {
                    start = start.plusWeeks(1);
                    end = end.plusWeeks(1);
                }
                case MONTHLY -> {
                    start = start.plusMonths(1);
                    end = end.plusMonths(1);
                }
                case YEARLY -> {
                    start = start.plusYears(1);
                    end = end.plusYears(1);
                }
                default -> start = repeatEnd.plusDays(1);
            }
        }

        sharedCalendarRepository.saveAll(schedules);
        return schedules.stream()
                .map(SharedCalendarResDto::fromEntity)
                .toList();
    }

    // 일정 조회
    public List<SharedCalendarResDto> getSchedules(UUID userId, String workspaceId) {
        // 1. 검증
        validationService.validateUserAndWorkspace(userId, workspaceId);

        // 2. 일정 조회
        List<SharedCalendar> calendars = sharedCalendarRepository.findByUserIdAndWorkspaceIdAndCalendarType(userId, workspaceId, CalendarType.SCHEDULE);

        List<SharedCalendar> expanded = new ArrayList<>();

        for (SharedCalendar base : calendars) {
            if (base.getRepeatCycle() == RepeatCycle.NONE) {
                expanded.add(base);
                continue;
            }

            if (base.getRepeatEndAt() == null || base.getRepeatEndAt().isBefore(base.getStartedAt()))
                continue;

            LocalDateTime current = base.getStartedAt();

            while (!current.isAfter(base.getRepeatEndAt())) {
                SharedCalendar copy = base.copyWithNewDate(current);
                expanded.add(copy);

                switch (base.getRepeatCycle()) {
                    case DAILY -> current = current.plusDays(1);
                    case WEEKLY -> current = current.plusWeeks(1);
                    case MONTHLY -> current = current.plusMonths(1);
                    case YEARLY -> current = current.plusYears(1);
                }
            }
        }

        return expanded.stream()
                .map(SharedCalendarResDto::fromEntity)
                .collect(Collectors.toMap(
                        SharedCalendarResDto::getId,
                        dto -> dto,
                        (existing, duplicate) -> existing // 중복일 경우 첫 번째 유지
                ))
                .values().stream().toList();
    }

    // 일정 수정
    // TODO: 반복 그룹 전체 수정 기능 추가 시 repeatGroupId 기반으로 처리
    public SharedCalendarResDto updateSchedule(String calendarId, UUID userId, UpdateScheduleReqDto dto) {
        // 1. 검증
        var calendar = sharedCalendarRepository.findById(calendarId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 일정입니다."));

        validationService.validateUserAndWorkspace(userId, calendar.getWorkspaceId());

        if (!calendar.getUserId().getId().equals(userId))
            throw new IllegalArgumentException("본인 일정만 수정할 수 있습니다.");

        // 2. 일정 수정
        // 2-1. 반복 그룹 전체 수정인 경우
        if (Boolean.TRUE.equals(dto.getApplyToGroup())) {
            List<SharedCalendar> groupEvents = sharedCalendarRepository.findByRepeatGroupId(calendar.getRepeatGroupId());
            for (SharedCalendar ev : groupEvents) {
                ev.updateSchedule(dto.getCalendarName(), dto.getStartedAt(), dto.getEndedAt(),
                        dto.getIsShared(), dto.getRepeatCycle(), dto.getRepeatEndAt());
            }
            return SharedCalendarResDto.fromEntity(calendar); // 아무 일정 하나를 기준으로 반환
        }

        // 2-2. 단일 일정만 수정
        calendar.updateSchedule(dto.getCalendarName(), dto.getStartedAt(), dto.getEndedAt(),
                dto.getIsShared(), dto.getRepeatCycle(), dto.getRepeatEndAt());
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

    // 반복 일정 종료 시, 이후 일정 삭제
    public void cutOffRepeat(UUID userId, String calendarId) {
        // 1. 기준 일정 찾기
        SharedCalendar base = sharedCalendarRepository.findById(calendarId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 일정입니다."));

        validationService.validateUserAndWorkspace(userId, base.getWorkspaceId());

        if (!base.getUserId().getId().equals(userId))
            throw new IllegalArgumentException("본인 일정만 수정할 수 있습니다.");

        // 2. 반복 그룹 찾기 (같은 이름 + workspace 기준)
        List<SharedCalendar> repeats = sharedCalendarRepository.findByRepeatGroupId(base.getRepeatGroupId());

        // 3. 기준 일정의 startedAt 이후 일정 삭제
        LocalDateTime baseStart = base.getStartedAt();

        repeats.stream()
                .filter(c -> c.getStartedAt().isAfter(baseStart) || c.getStartedAt().isEqual(baseStart))
                .forEach(sharedCalendarRepository::delete);

        // 4. 현재 일정의 repeatEndAt을 기준 일정의 startedAt으로 업데이트 (반복 중단)
        repeats.stream()
                .filter(c -> c.getStartedAt().isBefore(baseStart))
                .forEach(c -> c.updateSchedule(
                        c.getCalendarName(),
                        c.getStartedAt(),
                        c.getEndedAt(),
                        c.getIsShared(),
                        c.getRepeatCycle(),
                        baseStart   // 반복 종료일 조정
                ));
    }

    // 반복 일정 중 특정 일정만 삭제
    public void cancelOneRepeat(UUID userId, String calendarId) {
        // 1. 기준 일정 찾기
        SharedCalendar target = sharedCalendarRepository.findById(calendarId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 일정입니다."));

        validationService.validateUserAndWorkspace(userId, target.getWorkspaceId());

        if (!target.getUserId().getId().equals(userId))
            throw new IllegalArgumentException("본인 일정만 수정할 수 있습니다.");

        sharedCalendarRepository.delete(target);
    }

}