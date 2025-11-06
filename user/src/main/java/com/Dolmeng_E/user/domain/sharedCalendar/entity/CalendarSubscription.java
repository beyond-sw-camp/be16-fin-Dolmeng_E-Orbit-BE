package com.Dolmeng_E.user.domain.sharedCalendar.entity;

import com.Dolmeng_E.user.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "calendarSubscription_generator")
    @GenericGenerator(
            name = "calendarSubscription_generator", // generator 이름
            strategy = "com.Dolmeng_E.user.common.domain.StringPrefixedSequenceIdGenerator", // 1단계에서 만든 클래스 경로
            parameters = {
                    @org.hibernate.annotations.Parameter(name = "sequence_name", value = "calendarSubscription_seq"), // DB에 생성할 시퀀스 이름
                    @org.hibernate.annotations.Parameter(name = "initial_value", value = "1"), // 시퀀스 시작 값
                    @org.hibernate.annotations.Parameter(name = "increment_size", value = "1"), // 시퀀스 증가 값
                    @Parameter(name = "valuePrefix", value = "u_sub_") // ID에 붙일 접두사!
            }
    )
    private String id;

    // TODO 수정 필수
    @Column(length = 255, nullable = false)
    private String workspaceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscriber_user_id", nullable = false)
    private User subscriberUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    private User targetUserId;
}
