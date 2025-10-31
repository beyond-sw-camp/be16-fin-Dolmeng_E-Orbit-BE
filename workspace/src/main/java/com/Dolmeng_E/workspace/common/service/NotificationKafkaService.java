package com.Dolmeng_E.workspace.common.service;

import com.Dolmeng_E.workspace.common.dto.NotificationCreateReqDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class NotificationKafkaService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public NotificationKafkaService(
            @Qualifier("notificationKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void kafkaNotificationPublish(NotificationCreateReqDto dto) {
        log.info("kafkaNotificationPublish() - dto: {}", dto);
        try {
            String data = objectMapper.writeValueAsString(dto);
            kafkaTemplate.send("notification.publish", data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

