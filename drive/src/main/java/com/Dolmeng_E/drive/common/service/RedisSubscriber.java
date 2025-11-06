package com.Dolmeng_E.drive.common.service;

import com.Dolmeng_E.drive.common.dto.EditorBatchMessageDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessageSendingOperations messagingTemplate;

    // Redis에서 메시지가 발행될 때 호출되는 메서드
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String publishMessage = new String(message.getBody());
            EditorBatchMessageDto editorMessage = objectMapper.readValue(publishMessage, EditorBatchMessageDto.class);

            // 해당 문서를 구독하고 있는 클라이언트들에게 메시지 전송
            // STOMP의 destination은 /topic/document/{documentId} 형태
            String destination = "/topic/document/" + editorMessage.getDocumentId();
            messagingTemplate.convertAndSend(destination, editorMessage);

            log.info("메시지 발행 성공: {}"+" method : {}", editorMessage.getChangesList().toString(), editorMessage.getMessageType());

        } catch (Exception e) {
            log.error("메시지 처리 중 에러 발생", e);
        }
    }
}