package com.Dolmeng_E.drive.common.service;

import com.Dolmeng_E.drive.common.dto.EventDto;
import com.Dolmeng_E.drive.domain.drive.service.DriverService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class EventConsumer {

    private final ObjectMapper objectMapper;
    private final DriverService driverService;

    @KafkaListener(topics = "drive-delete-topic", groupId = "drive-consumer-group")
    public void handleDocument(String eventMessage, Acknowledgment ack) {
        try {
            // 1. Kafka 메시지(JSON)를 DTO로 파싱
            EventDto eventDto = objectMapper.readValue(eventMessage, EventDto.class);
            String rootType = eventDto.getRootType();
            String rootId = eventDto.getRootId();
            driverService.deleteAll(rootId, rootType);
            ack.acknowledge();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
