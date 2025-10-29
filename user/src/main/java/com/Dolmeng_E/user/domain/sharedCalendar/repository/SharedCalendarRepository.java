package com.Dolmeng_E.user.domain.sharedCalendar.repository;

import com.Dolmeng_E.user.domain.sharedCalendar.entity.CalendarType;
import com.Dolmeng_E.user.domain.sharedCalendar.entity.RepeatCycle;
import com.Dolmeng_E.user.domain.sharedCalendar.entity.SharedCalendar;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SharedCalendarRepository extends JpaRepository<SharedCalendar, String> {

    // 본인 일정 전용 조회
    // 해당 유저(userId)가 작성한 일정 중 특정 워크스페이스(workspaceId)에 속한 모든 일정 조회
    @Query("SELECT c FROM SharedCalendar c " +
            "WHERE c.userId.id = :userId " +
            "AND c.workspaceId = :workspaceId " +
            "AND c.calendarType = :calendarType")
    List<SharedCalendar> findByUserIdAndWorkspaceIdAndCalendarType(
            @Param("userId") UUID userId,
            @Param("workspaceId") String workspaceId,
            @Param("calendarType") CalendarType calendarType
    );

    // 구독자용 공유 일정 조회
    // 특정 유저(userId)가 공유(isShared=true)로 설정한 일정 중 해당 워크스페이스(workspaceId)에 속한 일정만 조회
    @Query("SELECT c FROM SharedCalendar c " +
            "WHERE c.userId.id = :userId " +
            "AND c.workspaceId = :workspaceId " +
            "AND c.calendarType = :calendarType")
    List<SharedCalendar> findSharedCalendarsByUserIdAndWorkspaceIdAndCalendarType(
            @Param("userId") UUID userId,
            @Param("workspaceId") String workspaceId,
            @Param("calendarType") CalendarType calendarType
    );

    // 반복 그룹으로 일정 조회
    List<SharedCalendar> findByRepeatGroupId(String repeatGroupId);

    // todo 조회용
    @Query("SELECT c FROM SharedCalendar c " +
            "WHERE c.userId.id = :userId " +
            "AND c.workspaceId = :workspaceId " +
            "AND c.calendarType = :calendarType")
    List<SharedCalendar> findTodosByUserIdAndWorkspaceIdAndCalendarType(
            @Param("userId") UUID userId,
            @Param("workspaceId") String workspaceId,
            @Param("calendarType") CalendarType calendarType
    );

    // todo 특정 날짜 조회
    @Query("SELECT s FROM SharedCalendar s " +
            "WHERE s.userId.id = :userId " +
            "AND s.workspaceId = :workspaceId " +
            "AND s.calendarType = :calendarType " +
            "AND s.startedAt <= :endOfDay " +
            "AND s.endedAt >= :startOfDay")
    List<SharedCalendar> findTodosByUserIdAndWorkspaceIdAndCalendarTypeAndDateRange(
            @Param("userId") UUID userId,
            @Param("workspaceId") String workspaceId,
            @Param("calendarType") CalendarType calendarType,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    @Query("""
        SELECT c FROM SharedCalendar c
        WHERE c.userId.id = :userId
            AND c.workspaceId = :workspaceId
            AND c.calendarType = :calendarType
            AND c.endedAt < :endedAt
            AND c.isCompleted = false
    """)
    List<SharedCalendar> findByUserIdAndWorkspaceIdAndCalendarTypeAndEndedAtBeforeAndNotCompleted(
            @Param("userId") UUID userId,
            @Param("workspaceId") String workspaceId,
            @Param("calendarType") CalendarType calendarType,
            @Param("endedAt") LocalDateTime endedAt
    );
}