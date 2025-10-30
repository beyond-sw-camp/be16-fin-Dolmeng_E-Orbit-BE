package com.Dolmeng_E.chat_db.common.service;

import com.Dolmeng_E.chat_db.common.dto.UserInfoResDto;
import com.Dolmeng_E.chat_db.domain.dto.ChatMessageDto;
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
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.ExecutionException;

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
    public void kafkaChatMessageSaved(ChatMessageDto dto) {
        log.info("kafkaMessageSaved() - dto: " + dto);
        try {
            String data = objectMapper.writeValueAsString(dto);
            kafkaTemplate.send("chat.message.saved",Long.toString(dto.getRoomId()), data);
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
    public void chatConsumer(@Header(KafkaHeaders.RECEIVED_KEY) String key, String message, Acknowledgment ack) throws JsonProcessingException {
        log.info("chatConsumer() - key : " + key + "message: " + message);

        // 채팅방
        ChatMessageDto chatMessageDto = objectMapper.readValue(message, ChatMessageDto.class);
        // sender정보 가져와서, 이름이랑 프로필 이미지 담기
        UserInfoResDto senderInfo = userFeignClient.fetchUserInfoById(chatMessageDto.getSenderId());
        chatMessageDto.setSenderName(senderInfo.getUserName());
        chatMessageDto.setUserProfileImageUrl(senderInfo.getProfileImageUrl());

        chatService.saveMessage(Long.parseLong(key), chatMessageDto);



        // 커밋 이후 실행되도록 예약
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                // chat.message.saved 메시지 발행
                kafkaChatMessageSaved(chatMessageDto);
            }
        });

        // 위 작업들 문제없으면 커밋 (offset)
        ack.acknowledge();
    }
}
