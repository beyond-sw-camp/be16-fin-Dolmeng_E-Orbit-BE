package com.Dolmeng_E.user.domain.notification.repository;

import com.Dolmeng_E.user.domain.notification.entity.Notification;
import com.Dolmeng_E.user.domain.notification.entity.NotificationReadStatus;
import com.Dolmeng_E.user.domain.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUser_IdAndReadStatusNotAndTypeNotOrderByCreatedAtDesc(
            UUID userId,
            NotificationReadStatus excludedStatus,
            NotificationType excludedType
    );
}
