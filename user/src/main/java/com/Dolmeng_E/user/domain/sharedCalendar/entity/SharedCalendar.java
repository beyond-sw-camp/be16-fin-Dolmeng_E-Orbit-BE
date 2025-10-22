package com.Dolmeng_E.user.domain.sharedCalendar.entity;

import com.Dolmeng_E.user.domain.user.entity.User;
import com.example.modulecommon.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.Dolmeng_E.user.domain.sharedCalendar.entity.CalendarType.SCHEDULE;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedCalendar extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sharedCalendar_generator")
    @GenericGenerator(
            name = "sharedCalendar_generator", // generator 이름
            strategy = "com.Dolmeng_E.user.common.domain.StringPrefixedSequenceIdGenerator", // 1단계에서 만든 클래스 경로
            parameters = {
                    @org.hibernate.annotations.Parameter(name = "sequence_name", value = "sharedCalendar_seq"), // DB에 생성할 시퀀스 이름
                    @org.hibernate.annotations.Parameter(name = "initial_value", value = "1"), // 시퀀스 시작 값
                    @org.hibernate.annotations.Parameter(name = "increment_size", value = "1"), // 시퀀스 증가 값
                    @Parameter(name = "valuePrefix", value = "u_cal_") // ID에 붙일 접두사!
            }
    )
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User userId;

    // TODO 수정 필수
    @Column(length = 255, nullable = false)
    private String workspaceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "calendar_type", nullable = false)
    private CalendarType calendarType;

    @Column(name = "calendar_name", length = 50, nullable = false)
    private String calendarName;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at", nullable = false)
    private LocalDateTime endedAt;

    // todo 즐겨찾기 여부
    @Column(nullable = false)
    @Builder.Default
    private Boolean bookmark = false;

    // todo 완료 여부
    @Column(nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    // 일정 공유 여부
    @Column(name = "is_shared", nullable = false)
    @Builder.Default
    private Boolean isShared = false;

    // 일정 반복 주기
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RepeatCycle repeatCycle = RepeatCycle.NONE;

    // 일정 반복 종료일 (없으면 무한반복 x)
    private LocalDateTime repeatEndAt;

    // 반복 그룹 식별자
    @Column(nullable = false, length = 36)
    private String repeatGroupId;

    @PrePersist
    public void prePersist() {
        if (this.repeatGroupId == null) {
            this.repeatGroupId = UUID.randomUUID().toString();
        }
    }

    // 일정 업데이트 (반복정보 포함)
    public void updateSchedule(String name, LocalDateTime start, LocalDateTime end,
                               Boolean isShared, RepeatCycle repeatCycle, LocalDateTime repeatEndAt) {
        this.calendarName = name;
        this.startedAt = start;
        this.endedAt = end;
        this.isShared = isShared;
        if (repeatCycle != null) this.repeatCycle = repeatCycle;
        this.repeatEndAt = repeatEndAt;
    }

    // 반복 일정 복제 후 날짜 변경
    public SharedCalendar copyWithNewDate(LocalDateTime newStart) {
        Duration duration = Duration.between(this.startedAt, this.endedAt);
        return SharedCalendar.builder()
                .id(this.id)
                .userId(this.userId)
                .workspaceId(this.workspaceId)
                .calendarType(this.calendarType)
                .calendarName(this.calendarName)
                .startedAt(newStart)
                .endedAt(newStart.plus(duration))
                .repeatCycle(this.repeatCycle)
                .repeatEndAt(this.repeatEndAt)
                .isShared(this.isShared)
                .bookmark(this.bookmark)
                .isCompleted(this.isCompleted)
                .repeatGroupId(this.repeatGroupId)
                .build();
    }

    // todo 정보 업데이트
    public void updateTodo(String name, LocalDate date, Boolean bookmark) {
        this.calendarName = name;

        // date → 하루 전체로 환산
        this.startedAt = date.atStartOfDay();             // 2025-10-21T00:00:00
        this.endedAt = date.atTime(23, 59, 59);           // 2025-10-21T23:59:59

        if (bookmark != null) {
            this.bookmark = bookmark;
        }
    }

    // todo 완료 업데이트
    public void completedTodo() {
        this.isCompleted = true;
    }

    // todo 미완료 업데이트
    public void incompletedTodo() {
        this.isCompleted = false;
    }
}
