package com.Dolmeng_E.workspace.domain.chatbot.repository;

import com.Dolmeng_E.workspace.domain.chatbot.entity.ChatbotMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatbotMessageRepository extends JpaRepository<ChatbotMessage, Long> {
}
