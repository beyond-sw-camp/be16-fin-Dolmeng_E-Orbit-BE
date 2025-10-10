package com.Dolmeng_E.chat.common.service;

import com.Dolmeng_E.chat.domain.dto.ChatMessageDto;
import com.Dolmeng_E.chat.domain.service.ChatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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
public class KafkaService {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final SimpMessageSendingOperations messageTemplate;
    private final ChatService chatService;

    // producer
    public void kafkaMessageKeyCreate(ChatMessageDto dto) {
        System.out.println("kafkaMessageKeyCreate: " + dto);
        try {
            System.out.println("kafkaMessageKeyCreate: " + dto);
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
        System.out.println("key 값 : " + key);
        System.out.println("컨슈머 메시지 수신1 : " + message);

        ChatMessageDto dto = objectMapper.readValue(message, ChatMessageDto.class);
//        chatService.saveMessage(Long.parseLong(key), dto);
        messageTemplate.convertAndSend("/topic/"+key, dto);

        // 위 작업들 문제없으면 커밋 (offset)
        ack.acknowledge();
    }
}
