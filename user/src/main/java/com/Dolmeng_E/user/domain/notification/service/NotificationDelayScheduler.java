package com.Dolmeng_E.user.domain.notification.service;

import com.Dolmeng_E.user.common.service.RedisPubSubService;
import com.Dolmeng_E.user.domain.notification.dto.NotificationCreateReqDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
public class NotificationDelayScheduler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisPubSubService redisPubSubService;
    private final ObjectMapper objectMapper;

    private static final String NOTIFY_QUEUE_KEY = "notification:delay";

    public NotificationDelayScheduler(@Qualifier("notificationInventory") RedisTemplate<String, Object> redisTemplate, RedisPubSubService redisPubSubService, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.redisPubSubService = redisPubSubService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedRate = 1000) // 1초마다 실행
    public void processDueNotifications() {
        long now = System.currentTimeMillis();
        // 현재 시각까지의 알림 검색
        Set<Object> dueNotifications = redisTemplate.opsForZSet()
                .rangeByScore(NOTIFY_QUEUE_KEY, 0, now);

        if (dueNotifications == null || dueNotifications.isEmpty()) return;

        for (Object notif : dueNotifications) {
            try {
                // Redis에서 제거 (원자적 제거 필요 시 Lua Script 사용 가능)
                redisTemplate.opsForZSet().remove(NOTIFY_QUEUE_KEY, notif);

                NotificationCreateReqDto dto =
                        objectMapper.readValue(notif.toString(), NotificationCreateReqDto.class);

                String payload = objectMapper.writeValueAsString(dto);
                redisPubSubService.publish("notification", payload);

                log.info("📩 예약 알림 발행 완료: {}", dto.getTitle());
            } catch (Exception e) {
                log.error("❌ 예약 알림 처리 실패", e);
            }
        }
    }
}

