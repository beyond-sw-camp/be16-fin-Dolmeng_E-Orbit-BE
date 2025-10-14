package com.Dolmeng_E.chat.domain.controller;

import com.Dolmeng_E.chat.common.service.KafkaService;
import com.Dolmeng_E.chat.domain.dto.ChatFileListDto;
import com.Dolmeng_E.chat.domain.dto.ChatMessageDto;
import com.Dolmeng_E.chat.domain.entity.ChatFile;
import com.Dolmeng_E.chat.domain.entity.ChatMessage;
import com.Dolmeng_E.chat.domain.entity.MessageType;
import com.Dolmeng_E.chat.domain.service.ChatService;
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

        ChatMessageDto chatMessageDto = chatService.saveMessage(roomId, dto);

        kafkaService.kafkaMessageKeyCreate(chatMessageDto);
    }
}
