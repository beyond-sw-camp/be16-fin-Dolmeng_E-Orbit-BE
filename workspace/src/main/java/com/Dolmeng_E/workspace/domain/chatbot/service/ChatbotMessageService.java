package com.Dolmeng_E.workspace.domain.chatbot.service;

import com.Dolmeng_E.workspace.domain.chatbot.repository.ChatbotMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatbotMessageService {
    private final ChatbotMessageRepository chatbotMessageRepository;
}
