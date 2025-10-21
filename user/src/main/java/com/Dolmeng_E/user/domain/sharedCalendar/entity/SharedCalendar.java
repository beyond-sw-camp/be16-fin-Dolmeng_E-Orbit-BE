package com.Dolmeng_E.user.domain.sharedCalendar.entity;

import com.Dolmeng_E.user.domain.user.entity.User;
import com.example.modulecommon.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
//    @Builder.Default
//    private CalendarType calendarType = SCHEDULE;
    private CalendarType calendarType;

    @Column(name = "calendar_name", length = 20, nullable = false)
    private String calendarName;

    @Column(name = "start_at", nullable = false)
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


    public void updateSchedule(String name, LocalDateTime start, LocalDateTime end, Boolean isShared) {
        this.calendarName = name;
        this.startedAt = start;
        this.endedAt = end;
        this.isShared = isShared;
    }

    public void updateTodo(String name, LocalDate date, Boolean bookmark) {
        this.calendarName = name;

        // date → 하루 전체로 환산
        this.startedAt = date.atStartOfDay();             // 2025-10-21T00:00:00
        this.endedAt = date.atTime(23, 59, 59);           // 2025-10-21T23:59:59

        if (bookmark != null) {
            this.bookmark = bookmark;
        }
    }
}
