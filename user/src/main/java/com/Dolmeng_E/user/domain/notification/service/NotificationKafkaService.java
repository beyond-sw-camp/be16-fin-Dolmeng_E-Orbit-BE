package com.Dolmeng_E.user.domain.notification.service;

import com.Dolmeng_E.user.common.service.RedisPubSubService;
import com.Dolmeng_E.user.domain.notification.dto.NotificationCreateReqDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificationKafkaService {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final RedisPubSubService redisPubSubService;

    // producer
    public void kafkaNotificationPublish(NotificationCreateReqDto dto) {
        log.info("kafkaNotificationPublish() - dto: " + dto);
        try {
            String data = objectMapper.writeValueAsString(dto);
            kafkaTemplate.send("notification.publish", data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    // consumer
    @KafkaListener(
            topics = "notification.publish",
            groupId = "notification-group",
            containerFactory = "notificationKafkaListener"
    )
    public void NotificationConsumer(String message, Acknowledgment ack) throws JsonProcessingException {
        log.info("kafkaListener2 - NotificationConsumer() - message: " + message);

        NotificationCreateReqDto reqDto = objectMapper.readValue(message, NotificationCreateReqDto.class);

        // 즉시 알림인지, 예약 알림인지 구분
        if(reqDto.getSendAt() == null) { // 즉시 알림
            // DB저장
            notificationService.createNotification(reqDto);

            // Pub/Sub 전파
            String pubsubMessage = objectMapper.writeValueAsString(reqDto);
            redisPubSubService.publish("notification", pubsubMessage);
        } else { // 예약 알림
            // DB저장
            notificationService.createReservationNotification(reqDto);
        }

        // 위 작업들 문제없으면 커밋 (offset)
        ack.acknowledge();
    }

}