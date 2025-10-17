package com.Dolmeng_E.user.domain.sharedCalendar.controller;

import com.Dolmeng_E.user.domain.sharedCalendar.dto.CreateScheduleReqDto;
import com.Dolmeng_E.user.domain.sharedCalendar.dto.SharedCalendarResDto;
import com.Dolmeng_E.user.domain.sharedCalendar.dto.UpdateScheduleReqDto;
import com.Dolmeng_E.user.domain.sharedCalendar.service.SharedCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/shared-calendars")
@RequiredArgsConstructor
public class SharedCalendarController {

    private final SharedCalendarService sharedCalendarService;

    // 일정 등록
    @PostMapping
    public SharedCalendarResDto createSchedule(@RequestHeader("X-User-Id") String userId,
                                               @RequestBody CreateScheduleReqDto dto) {
        return sharedCalendarService.createSchedule(UUID.fromString(userId), dto);
    }

    // 일정 조회
    @GetMapping
    public List<SharedCalendarResDto> getSchedules(@RequestHeader("X-User-Id") String userId) {
        return sharedCalendarService.getSchedules(UUID.fromString(userId));
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
}