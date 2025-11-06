package com.Dolmeng_E.workspace.domain.chatbot.repository;

import com.Dolmeng_E.workspace.domain.chatbot.entity.ChatbotMessage;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatbotMessageRepository extends JpaRepository<ChatbotMessage, Long> {
    List<ChatbotMessage> findByWorkspaceParticipant(WorkspaceParticipant workspaceParticipant);
    List<ChatbotMessage> findByWorkspaceParticipant(
            WorkspaceParticipant workspaceParticipant,
            Pageable pageable
    );
}
