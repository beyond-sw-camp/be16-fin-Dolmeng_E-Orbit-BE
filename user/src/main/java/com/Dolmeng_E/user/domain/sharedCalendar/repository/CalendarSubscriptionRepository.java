package com.Dolmeng_E.user.domain.sharedCalendar.repository;

import com.Dolmeng_E.user.domain.sharedCalendar.entity.CalendarSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CalendarSubscriptionRepository extends JpaRepository<CalendarSubscription, String> {

    @Query("SELECT s.targetUserId.id FROM CalendarSubscription s WHERE s.subscriberUserId.id = :userId")
    List<UUID> findTargetUserIdsBySubscriber(UUID userId);

    // 중복 구독 검증
    boolean existsBySubscriberUserId_IdAndTargetUserId_IdAndWorkspaceId(UUID subscriberId, UUID targetId, String workspaceId);

}