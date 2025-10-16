package com.Dolmeng_E.workspace.domain.chatbot.controller;

import com.Dolmeng_E.workspace.domain.chatbot.service.ChatbotMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
public class ChatbotMessageController {
    private final ChatbotMessageService chatbotMessageService;
}
