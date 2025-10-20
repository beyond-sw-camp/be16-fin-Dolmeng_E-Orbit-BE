package com.Dolmeng_E.user.domain.sharedCalendar.repository;

import com.Dolmeng_E.user.domain.sharedCalendar.entity.SharedCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface SharedCalendarRepository extends JpaRepository<SharedCalendar, String> {

    // 특정 유저의 일정 (본인 일정 + 공유된 일정)
    @Query("SELECT c FROM SharedCalendar c WHERE c.userId.id = :userId OR c.isShared = true")
    List<SharedCalendar> findByUserIdOrShared(UUID userId);

    // 구독한 유저들의 공유 일정
    @Query("SELECT c FROM SharedCalendar c WHERE c.userId.id IN :targetUserIds AND c.isShared = true")
    List<SharedCalendar> findSharedCalendarsOfSubscribedUsers(List<UUID> targetUserIds);

    // 특정 유저가 공유한 일정만 조회하는 쿼리 메서드 추가
    @Query("SELECT c FROM SharedCalendar c WHERE c.userId.id = :userId AND c.isShared = true")
    List<SharedCalendar> findSharedCalendarsByUserId(UUID userId);
}