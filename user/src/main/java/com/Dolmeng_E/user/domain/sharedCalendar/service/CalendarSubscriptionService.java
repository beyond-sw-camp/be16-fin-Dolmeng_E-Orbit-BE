package com.Dolmeng_E.user.domain.sharedCalendar.service;

import com.Dolmeng_E.user.common.service.WorkspaceFeign;
import com.Dolmeng_E.user.domain.sharedCalendar.dto.SubscriptionCreateReqDto;
import com.Dolmeng_E.user.domain.sharedCalendar.dto.SubscriptionDeleteReqDto;
import com.Dolmeng_E.user.domain.sharedCalendar.dto.SubscriptionResDto;
import com.Dolmeng_E.user.domain.sharedCalendar.entity.CalendarSubscription;
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

    // 구독 추가
    public List<SubscriptionResDto> subscribeList(UUID subscriberId, SubscriptionCreateReqDto dto) {
        User subscriber = userRepository.findById(subscriberId)
                .orElseThrow(() -> new IllegalArgumentException("구독자 정보를 찾을 수 없습니다."));

        if (!workspaceFeign.checkWorkspaceExists(dto.getWorkspaceId())) {
            throw new IllegalArgumentException("존재하지 않는 워크스페이스입니다.");
        }

        if (!workspaceFeign.checkWorkspaceMembership(dto.getWorkspaceId(), subscriberId)) {
            throw new IllegalArgumentException("구독자는 해당 워크스페이스에 속하지 않습니다.");
        }

        List<SubscriptionResDto> result = new ArrayList<>();

        for (UUID targetId : dto.getTargetUserIdList()) {
            User target = userRepository.findById(targetId)
                    .orElseThrow(() -> new IllegalArgumentException("대상 유저를 찾을 수 없습니다."));

            if (!workspaceFeign.checkWorkspaceMembership(dto.getWorkspaceId(), targetId)) {
                throw new IllegalArgumentException("대상 유저가 워크스페이스에 속하지 않습니다.");
            }

            // 중복 구독 여부 검증
            boolean alreadySubscribed = calendarSubscriptionRepository.existsBySubscriberUserId_IdAndTargetUserId_IdAndWorkspaceId(
                    subscriberId, targetId, dto.getWorkspaceId()
            );

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

    // 구독 리스트 조회
    public List<SubscriptionResDto> getSubscriptions(UUID userId) {
        // 구독 리스트 조회
        List<CalendarSubscription> list = calendarSubscriptionRepository.findAll().stream()
                .filter(sub -> sub.getSubscriberUserId().getId().equals(userId))
                .collect(Collectors.toList());

        // 첫 번째 구독 기준으로 워크스페이스 검증
        String workspaceId = list.get(0).getWorkspaceId();

        if (!workspaceFeign.checkWorkspaceExists(workspaceId)) {
            throw new IllegalArgumentException("존재하지 않는 워크스페이스입니다.");
        }

        if (!workspaceFeign.checkWorkspaceMembership(workspaceId, userId)) {
            throw new IllegalArgumentException("해당 유저는 워크스페이스에 속하지 않습니다.");
        }
        // 각 구독 대상의 공유 일정 목록 조회
        List<SubscriptionResDto> result = new ArrayList<>();

        for (CalendarSubscription sub : list) {
            List<SharedCalendar> sharedCalendars =
                    sharedCalendarRepository.findSharedCalendarsByUserId(sub.getTargetUserId().getId());


            // 일정 요약 DTO로 변환
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

            // 워크스페이스 검증
            String workspaceId = sub.getWorkspaceId();

            if (!workspaceFeign.checkWorkspaceExists(workspaceId)) {
                throw new IllegalArgumentException("존재하지 않는 워크스페이스입니다.");
            }

            if (!workspaceFeign.checkWorkspaceMembership(workspaceId, userId)) {
                throw new IllegalArgumentException("해당 유저는 워크스페이스에 속하지 않습니다.");
            }

            // 구독 검증
            if (!sub.getSubscriberUserId().getId().equals(userId))
                throw new IllegalArgumentException("본인 구독만 삭제할 수 있습니다.");

            // 구독 삭제
            calendarSubscriptionRepository.delete(sub);
        }
    }
}