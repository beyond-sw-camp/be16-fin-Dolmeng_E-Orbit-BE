package com.Dolmeng_E.user.domain.sharedCalendar.controller;

import com.Dolmeng_E.user.domain.sharedCalendar.dto.CreateScheduleReqDto;
import com.Dolmeng_E.user.domain.sharedCalendar.dto.SharedCalendarResDto;
import com.Dolmeng_E.user.domain.sharedCalendar.dto.UpdateScheduleReqDto;
import com.Dolmeng_E.user.domain.sharedCalendar.service.SharedCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/shared-calendars")
@RequiredArgsConstructor
public class SharedCalendarController {

    private final SharedCalendarService sharedCalendarService;

    // 일정 등록
    @PostMapping
    public List<SharedCalendarResDto> createSchedule(@RequestHeader("X-User-Id") String userId,
                                               @RequestBody CreateScheduleReqDto dto) {
        return sharedCalendarService.createSchedule(UUID.fromString(userId), dto);
    }

    // 일정 조회
    @GetMapping("/{workspaceId}")
    public List<SharedCalendarResDto> getSchedules(@RequestHeader("X-User-Id") String userId,
                                                   @PathVariable String workspaceId) {
        return sharedCalendarService.getSchedules(UUID.fromString(userId), workspaceId);
    }

    // 일정 수정
    @PutMapping("/{calendarId}")
    public SharedCalendarResDto updateSchedule(@RequestHeader("X-User-Id") String userId,
                                               @PathVariable String calendarId,
                                               @RequestBody UpdateScheduleReqDto dto) {
        return sharedCalendarService.updateSchedule(calendarId, UUID.fromString(userId), dto);
    }

    // 일정 삭제
    @DeleteMapping("/{calendarId}")
    public void deleteSchedule(@RequestHeader("X-User-Id") String userId,
                               @PathVariable String calendarId) {
        sharedCalendarService.deleteSchedule(calendarId, UUID.fromString(userId));
    }

    // 반복 일정 종료 시, 이후 일정 삭제
    @DeleteMapping("/{calendarId}/cut-off")
    public void cutOffRepeat(@RequestHeader("X-User-Id") String userId,
                             @PathVariable String calendarId) {
        sharedCalendarService.cutOffRepeat(UUID.fromString(userId), calendarId);
    }

    // 반복 일정 중 특정 일정만 삭제
    @DeleteMapping("/{calendarId}/cancel-one")
    public void cancelOneRepeat(@RequestHeader("X-User-Id") String userId,
                                @PathVariable String calendarId) {
        sharedCalendarService.cancelOneRepeat(UUID.fromString(userId), calendarId);
    }
}