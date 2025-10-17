package com.Dolmeng_E.chat.domain.controller;

import com.Dolmeng_E.chat.domain.dto.ChatbotUnreadMessageListReqDto;
import com.Dolmeng_E.chat.domain.service.ChatService;
import com.example.modulecommon.dto.CommonSuccessDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
public class AgentController {
    private final ChatService chatService;

    // 워크스페이스 내에서 사용자가 읽지 않은 메시지 전부 조회
    @GetMapping("/unread-messages")
    public ResponseEntity<?> getUnreadMessages(@ModelAttribute ChatbotUnreadMessageListReqDto dto) {
        String unreadMessages = chatService.getUnreadMessages(dto);
        return new ResponseEntity(new CommonSuccessDto(unreadMessages, HttpStatus.OK.value(), "읽지 않은 메시지 조회 성공"), HttpStatus.OK);
    }


}
