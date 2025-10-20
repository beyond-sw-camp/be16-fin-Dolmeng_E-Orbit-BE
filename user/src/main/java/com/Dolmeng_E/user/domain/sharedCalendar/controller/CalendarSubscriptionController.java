package com.Dolmeng_E.user.domain.sharedCalendar.controller;

import com.Dolmeng_E.user.domain.sharedCalendar.dto.*;
import com.Dolmeng_E.user.domain.sharedCalendar.service.CalendarSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
public class CalendarSubscriptionController {

    private final CalendarSubscriptionService calendarSubscriptionService;

    // 공유캘린더 구독 추가
    @PostMapping
    public List<SubscriptionResDto> subscribe(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody SubscriptionCreateReqDto dto
    ) {
        return calendarSubscriptionService.subscribeList(UUID.fromString(userId), dto);
    }

    // 공유 캘린더 구독 리스트 조회
    @GetMapping("/{workspaceId}")
    public List<SubscriptionResDto> getSubscriptions(@RequestHeader("X-User-Id") String userId,
                                                     @PathVariable String workspaceId) {
        return calendarSubscriptionService.getSubscriptions(UUID.fromString(userId), workspaceId);
    }

//    // 구독 수정
//    @PutMapping
//    public List<SubscriptionResDto> updateSubscriptions(
//            @RequestHeader("X-User-Id") String userId,
//            @RequestBody SubscriptionUpdateReqDto dto
//    ) {
//        return calendarSubscriptionService.updateSubscriptions(UUID.fromString(userId), dto);
//    }

    // 공유캘린더 구독 삭제
    @DeleteMapping
    public void deleteSubscriptions(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody SubscriptionDeleteReqDto dto
    ) {
        calendarSubscriptionService.deleteSubscriptions(UUID.fromString(userId), dto);
    }
}