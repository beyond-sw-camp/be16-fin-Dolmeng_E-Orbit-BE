package com.Dolmeng_E.chat.domain.controller;

import com.Dolmeng_E.chat.domain.dto.ChatMessageDto;
import com.Dolmeng_E.chat.domain.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class StompController {
    private final SimpMessageSendingOperations messageTemplate;
    private final ChatService chatService;

    @MessageMapping("/{roomId}")
    public void sendMessage(@DestinationVariable Long roomId, ChatMessageDto dto) {
        System.out.println("sender: " + dto.getSenderEmail() + ", message: " + dto.getMessage());

        chatService.saveMessage(roomId, dto);

        messageTemplate.convertAndSend("/topic/"+roomId, dto);
    }
}
