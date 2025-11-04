package com.Dolmeng_E.user.domain.notification.service;

import com.Dolmeng_E.user.domain.notification.dto.NotificationCreateReqDto;
import com.Dolmeng_E.user.domain.notification.dto.NotificationListResDto;
import com.Dolmeng_E.user.domain.notification.entity.Notification;
import com.Dolmeng_E.user.domain.notification.entity.NotificationReadStatus;
import com.Dolmeng_E.user.domain.notification.entity.NotificationType;
import com.Dolmeng_E.user.domain.notification.repository.NotificationRepository;
import com.Dolmeng_E.user.domain.user.entity.User;
import com.Dolmeng_E.user.domain.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String NOTIFY_QUEUE_KEY = "notification:delay";

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository, @Qualifier("notificationInventory") RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    // 알림 생성
    public void createNotification(NotificationCreateReqDto reqDto) {
        for(UUID userId : reqDto.getUserIdList()) {
            User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("없는 사용자입니다."));

            Notification notification = Notification.builder()
                    .title(reqDto.getTitle())
                    .content(reqDto.getContent())
                    .type(NotificationType.valueOf(reqDto.getType()))
                    .user(user)
                    .workspaceId(reqDto.getWorkspaceId())
                    .projectId(reqDto.getProjectId())
                    .stoneId(reqDto.getStoneId())
                    .taskId(reqDto.getTaskId())
                    .build();

            notificationRepository.save(notification);
        }
    }

    // 예약 알림 생성
    public void createReservationNotification(NotificationCreateReqDto reqDto) {
        for(UUID userId : reqDto.getUserIdList()) {
            User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("없는 사용자입니다."));

            Notification notification = Notification.builder()
                    .title(reqDto.getTitle())
                    .content(reqDto.getContent())
                    .type(NotificationType.valueOf(reqDto.getType()))
                    .user(user)
                    .build();

            // RDB 저장
            notificationRepository.save(notification);

            // redis zset 저장
            try {
                String json = objectMapper.writeValueAsString(reqDto);
                double score = reqDto.getSendAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                redisTemplate.opsForZSet().add(NOTIFY_QUEUE_KEY, json, score);
                log.info("예약 알림 등록: {} ({}점수)", reqDto.getTitle(), score);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // 알림 목록 조회
    public List<NotificationListResDto> getNotifications(String userId) {
        List<Notification> notificationList = notificationRepository.findByUser_IdAndReadStatusNotAndTypeNotOrderByCreatedAtDesc(UUID.fromString(userId)
        , NotificationReadStatus.REMOVE, NotificationType.NEW_CHAT_MESSAGE);

        List<NotificationListResDto> notificationListResDtoList = notificationList.stream().map(n -> NotificationListResDto.fromEntiry(n)).toList();
        return notificationListResDtoList;
    }

    // 알림 읽음 처리
    public void readNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElseThrow(() -> new EntityNotFoundException("없는 알림입니다."));
        notification.updateReadStatus(NotificationReadStatus.READ);
    }

    // 알림 삭제 처리
    public void removeNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElseThrow(() -> new EntityNotFoundException("없는 알림입니다."));
        notification.updateReadStatus(NotificationReadStatus.REMOVE);
    }
}
