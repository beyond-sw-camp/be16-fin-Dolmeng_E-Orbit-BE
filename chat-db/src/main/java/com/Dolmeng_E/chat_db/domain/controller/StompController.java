package com.Dolmeng_E.chat_db.domain.controller;

import com.Dolmeng_E.chat_db.common.service.KafkaService;
import com.Dolmeng_E.chat_db.domain.dto.ChatMessageDto;
import com.Dolmeng_E.chat_db.domain.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class StompController {
    private final SimpMessageSendingOperations messageTemplate;
    private final ChatService chatService;
    private final KafkaService kafkaService;

    @MessageMapping("/{roomId}")
    public void sendMessage(@DestinationVariable Long roomId, ChatMessageDto dto) {
        log.info("sendMessage() - roomId: " + roomId + ", dto : " + dto);

        // roomId 붙여서 카프카 메시지 발행
        dto.setRoomId(roomId);
        kafkaService.kafkaMessageKeyCreate(dto);
    }
}
