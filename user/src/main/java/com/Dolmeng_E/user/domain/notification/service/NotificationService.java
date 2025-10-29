package com.Dolmeng_E.user.domain.notification.service;

import com.Dolmeng_E.user.domain.notification.dto.NotificationCreateReqDto;
import com.Dolmeng_E.user.domain.notification.entity.Notification;
import com.Dolmeng_E.user.domain.notification.repository.NotificationRepository;
import com.Dolmeng_E.user.domain.user.entity.User;
import com.Dolmeng_E.user.domain.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // 알림 생성
    public void createNotification(NotificationCreateReqDto reqDto) {
        for(UUID userId : reqDto.getUserIdList()) {
            User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("없는 사용자입니다."));

            Notification notification = Notification.builder()
                    .title(reqDto.getTitle())
                    .content(reqDto.getContent())
                    .type(reqDto.getType())
                    .user(user)
                    .build();

            notificationRepository.save(notification);
        }
    }
}
