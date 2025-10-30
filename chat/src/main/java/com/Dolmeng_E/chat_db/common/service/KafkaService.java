package com.Dolmeng_E.chat_db.common.service;

import com.Dolmeng_E.chat_db.common.dto.NotificationCreateReqDto;
import com.Dolmeng_E.chat_db.common.dto.UserInfoResDto;
import com.Dolmeng_E.chat_db.domain.dto.ChatMessageDto;
import com.Dolmeng_E.chat_db.domain.entity.MessageType;
import com.Dolmeng_E.chat_db.domain.feignclient.UserFeignClient;
import com.Dolmeng_E.chat_db.domain.service.ChatService;
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
    private final ChatService chatService;
    private final UserFeignClient userFeignClient;

    // producer
    // 알림 발행
    public void kafkaNotificationPublish(NotificationCreateReqDto dto) {
        log.info("kafkaNotificationPublish() - dto: " + dto);
        try {
            String data = objectMapper.writeValueAsString(dto);
            kafkaTemplate.send("notification.publish", data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void kafkaChatMessageSent(ChatMessageDto dto) {
        log.info("kafkaChatMessageSent() - dto: " + dto);
        try {
            String data = objectMapper.writeValueAsString(dto);
            kafkaTemplate.send("chat.message.sent",Long.toString(dto.getRoomId()), data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    // consumer
    @KafkaListener(
            topics = "chat.message.sent",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListener"
    )
    public void sentChatConsumer(@Header(KafkaHeaders.RECEIVED_KEY) String key, String message, Acknowledgment ack) throws JsonProcessingException {
        log.info("sentChatConsumer() - key : " + key + "message: " + message);

        // 채팅방
        ChatMessageDto chatMessageDto = objectMapper.readValue(message, ChatMessageDto.class);
        // sender정보 가져와서, 이름이랑 프로필 이미지 담기
        UserInfoResDto senderInfo = userFeignClient.fetchUserInfoById(chatMessageDto.getSenderId());

        chatMessageDto.setSenderName(senderInfo.getUserName());
        chatMessageDto.setUserProfileImageUrl(senderInfo.getProfileImageUrl());

        if(chatMessageDto.getMessageType() == MessageType.TEXT) {
            messageTemplate.convertAndSend("/topic/"+key, chatMessageDto);

            // 위 작업들 문제없으면 커밋 (offset)
            ack.acknowledge();
        }
    }

    @KafkaListener(
            topics = "chat.message.saved",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListener"
    )
    public void savedChatConsumer(@Header(KafkaHeaders.RECEIVED_KEY) String key, String message, Acknowledgment ack) throws JsonProcessingException {
        log.info("savedChatConsumer() - key : " + key + "message: " + message);

        ChatMessageDto chatMessageDto = objectMapper.readValue(message, ChatMessageDto.class);

        // 먼저 알림 발행
        for(NotificationCreateReqDto dto : chatService.createNotification(chatMessageDto)) {
            kafkaNotificationPublish(dto);
        }

        // 채팅방 목록
        chatService.broadcastChatSummary(chatMessageDto);
        // sender정보 가져와서, 이름이랑 프로필 이미지 담기
        UserInfoResDto senderInfo = userFeignClient.fetchUserInfoById(chatMessageDto.getSenderId());
        chatMessageDto.setSenderName(senderInfo.getUserName());
        chatMessageDto.setUserProfileImageUrl(senderInfo.getProfileImageUrl());

        if(chatMessageDto.getMessageType() == MessageType.FILE) {
            messageTemplate.convertAndSend("/topic/"+key, chatMessageDto);

            // 위 작업들 문제없으면 커밋 (offset)
            ack.acknowledge();
        } else if(chatMessageDto.getMessageType() == MessageType.TEXT_WITH_FILE) {
            messageTemplate.convertAndSend("/topic/"+key, chatMessageDto);
            ack.acknowledge();
        }

    }
}
