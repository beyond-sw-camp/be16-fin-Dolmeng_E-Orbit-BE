package com.Dolmeng_E.user.common.service;

import com.Dolmeng_E.user.domain.notification.dto.NotificationCreateReqDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RedisPubSubService implements MessageListener {
    private final StringRedisTemplate stringRedisTemplate;
    private final SimpMessageSendingOperations messageTemplate;
    private final ObjectMapper objectMapper;

    public RedisPubSubService(@Qualifier("notificationPubSub") StringRedisTemplate stringRedisTemplate, SimpMessageSendingOperations messageTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.messageTemplate = messageTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(String channel, String message) {
        stringRedisTemplate.convertAndSend(channel, message);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody());

        try {
            NotificationCreateReqDto reqDto = objectMapper.readValue(payload, NotificationCreateReqDto.class);
            for(UUID userId : reqDto.getUserIdList()) {
                messageTemplate.convertAndSend("/topic/notification/"+userId, reqDto);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}