package com.Dolmeng_E.user.domain.sharedCalendar.repository;

import com.Dolmeng_E.user.domain.sharedCalendar.entity.CalendarSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CalendarSubscriptionRepository extends JpaRepository<CalendarSubscription, String> {

    // 구독유저 id와 워크스페이스 id 조회
    List<CalendarSubscription> findBySubscriberUserId_IdAndWorkspaceId(UUID subscriberId, String workspaceId);

    // 중복 구독 검증
    boolean existsBySubscriberUserId_IdAndTargetUserId_IdAndWorkspaceId(UUID subscriberId, UUID targetId, String workspaceId);

}