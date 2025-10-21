package com.Dolmeng_E.user.domain.sharedCalendar.service;

import com.Dolmeng_E.user.common.service.WorkspaceFeign;
import com.Dolmeng_E.user.domain.sharedCalendar.dto.SubscriptionCreateReqDto;
import com.Dolmeng_E.user.domain.sharedCalendar.dto.SubscriptionDeleteReqDto;
import com.Dolmeng_E.user.domain.sharedCalendar.dto.SubscriptionResDto;
import com.Dolmeng_E.user.domain.sharedCalendar.entity.CalendarSubscription;
import com.Dolmeng_E.user.domain.sharedCalendar.entity.CalendarType;
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
public class CalendarSubscriptionService {

    private final CalendarSubscriptionRepository calendarSubscriptionRepository;
    private final UserRepository userRepository;
    private final WorkspaceFeign workspaceFeign;
    private final SharedCalendarRepository sharedCalendarRepository;
    private final CalendarValidationService validationService;

    // 구독 추가
    public List<SubscriptionResDto> subscribeList(UUID subscriberId, SubscriptionCreateReqDto dto) {
        // 1. 구독자 + 워크스페이스 검증
        User subscriber = validationService.validateUserAndWorkspace(subscriberId, dto.getWorkspaceId());

        List<SubscriptionResDto> result = new ArrayList<>();

        // 2. 구독 대상 유저 반복 처리
        for (UUID targetId : dto.getTargetUserIdList()) {
            // 대상 유저 존재 여부 및 워크스페이스 멤버 검증
            User target = validationService.validateUser(targetId);
            validationService.validateMember(dto.getWorkspaceId(), targetId);

            // 3. 중복 구독 여부 검증
            boolean alreadySubscribed = calendarSubscriptionRepository.existsBySubscriberUserId_IdAndTargetUserId_IdAndWorkspaceId(
                    subscriberId, targetId, dto.getWorkspaceId()
            );

            if (alreadySubscribed) {
                throw new IllegalArgumentException("이미 구독한 사용자입니다.");
            }

            // 4. 구독 생성 및 저장
            CalendarSubscription subscription = CalendarSubscription.builder()
                    .workspaceId(dto.getWorkspaceId())
                    .subscriberUserId(subscriber)
                    .targetUserId(target)
                    .build();

            calendarSubscriptionRepository.save(subscription);
            result.add(SubscriptionResDto.fromEntity(subscription));
        }

        return result;
    }

    // 구독 일정 조회
    public List<SubscriptionResDto> getSubscriptions(UUID userId, String workspaceId) {

        // 1️. 유저 + 워크스페이스 검증
        validationService.validateUserAndWorkspace(userId, workspaceId);

        // 2. 해당 워크스페이스의 구독 리스트만 조회
        List<CalendarSubscription> list =
                calendarSubscriptionRepository.findBySubscriberUserId_IdAndWorkspaceId(userId, workspaceId);

        if (list.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 각 구독 대상의 공유 일정 조회
        List<SubscriptionResDto> result = new ArrayList<>();

        for (CalendarSubscription sub : list) {
            // 구독 대상의 일정 중 같은 워크스페이스에 속한 일정만 가져오기
            List<SharedCalendar> sharedCalendars =
                    sharedCalendarRepository.findSharedCalendarsByUserIdAndWorkspaceIdAndCalendarType(
                            sub.getTargetUserId().getId(), workspaceId, CalendarType.SCHEDULE
                    );

            List<SubscriptionResDto.SharedCalendarSubDto> briefList = sharedCalendars.stream()
                    .map(SubscriptionResDto.SharedCalendarSubDto::fromEntity)
                    .collect(Collectors.toList());

            result.add(SubscriptionResDto.fromEntity(sub, briefList));
        }

        return result;
    }

//    // 구독 수정
//    public List<SubscriptionResDto> updateSubscriptions(UUID userId, SubscriptionUpdateReqDto dto) {
//        if (dto.getSubscriptionIdList().size() != dto.getNewTargetUserIdList().size()) {
//            throw new IllegalArgumentException("수정할 구독 ID 수와 대상 유저 수가 일치하지 않습니다.");
//        }
//
//        List<SubscriptionResDto> updatedList = new ArrayList<>();
//
//        for (int i = 0; i < dto.getSubscriptionIdList().size(); i++) {
//            String subId = dto.getSubscriptionIdList().get(i);
//            UUID newTargetId = dto.getNewTargetUserIdList().get(i);
//
//            CalendarSubscription sub = calendarSubscriptionRepository.findById(subId)
//                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독입니다."));
//
//            if (!sub.getSubscriberUserId().getId().equals(userId))
//                throw new IllegalArgumentException("본인 구독만 수정할 수 있습니다.");
//
//            User newTarget = userRepository.findById(newTargetId)
//                    .orElseThrow(() -> new IllegalArgumentException("대상 유저를 찾을 수 없습니다."));
//
//            sub = CalendarSubscription.builder()
//                    .id(sub.getId())
//                    .workspaceId(sub.getWorkspaceId())
//                    .subscriberUserId(sub.getSubscriberUserId())
//                    .targetUserId(newTarget)
//                    .build();
//
//            calendarSubscriptionRepository.save(sub);
//            updatedList.add(SubscriptionResDto.fromEntity(sub));
//        }
//
//        return updatedList;
//    }

    // 구독 삭제
    public void deleteSubscriptions(UUID userId, SubscriptionDeleteReqDto dto) {
        for (String subId : dto.getSubscriptionIdList()) {
            CalendarSubscription sub = calendarSubscriptionRepository.findById(subId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독입니다."));

            String workspaceId = sub.getWorkspaceId();

            // 2. 워크스페이스 및 소속 검증
            validationService.validateWorkspace(workspaceId);
            validationService.validateMember(workspaceId, userId);

            // 구독 여부 검증
            if (!sub.getSubscriberUserId().getId().equals(userId))
                throw new IllegalArgumentException("구독하지 않은 유저의 ID입니다.");

            // 구독 삭제
            calendarSubscriptionRepository.delete(sub);
        }
    }
}