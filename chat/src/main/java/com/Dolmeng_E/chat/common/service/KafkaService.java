package com.Dolmeng_E.chat.common.service;

import com.Dolmeng_E.chat.domain.dto.ChatMessageDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class KafkaService {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final SimpMessageSendingOperations messageTemplate;

    // producer
    public void kafkaMessageKeyCreate(ChatMessageDto dto) {
        log.info("kafkaMessageKeyCreate() - dto: " + dto);
        try {
            String data = objectMapper.writeValueAsString(dto);
            kafkaTemplate.send("chat-service",Long.toString(dto.getRoomId()), data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    // consumer
    @KafkaListener(
            topics = "chat-service",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListener"
    )
    public void chatConsumer(@Header(KafkaHeaders.RECEIVED_KEY) String key, String message, Acknowledgment ack) throws JsonProcessingException {
        log.info("chatConsumer() - key : " + key + "message: " + message);

        ChatMessageDto dto = objectMapper.readValue(message, ChatMessageDto.class);
//        chatService.saveMessage(Long.parseLong(key), dto);
        messageTemplate.convertAndSend("/topic/"+key, dto);

        // 위 작업들 문제없으면 커밋 (offset)
        ack.acknowledge();
    }
}
