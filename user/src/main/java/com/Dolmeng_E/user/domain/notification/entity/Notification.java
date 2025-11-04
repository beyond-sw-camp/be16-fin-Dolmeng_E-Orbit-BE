package com.Dolmeng_E.user.domain.notification.entity;

import com.Dolmeng_E.user.domain.user.entity.User;
import com.example.modulecommon.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String content;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(nullable = false)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private NotificationReadStatus readStatus = NotificationReadStatus.UNREAD;

    @JoinColumn(name = "user_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private String workspaceId;
    private String projectId;
    private String stoneId;
    private String taskId;

    public void updateReadStatus(NotificationReadStatus readStatus) {
        this.readStatus = readStatus;
    }
}
