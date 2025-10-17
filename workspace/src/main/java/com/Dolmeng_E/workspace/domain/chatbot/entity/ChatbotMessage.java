package com.Dolmeng_E.workspace.domain.chatbot.entity;

import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import com.example.modulecommon.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ChatbotMessage extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ChatbotMessageType type =  ChatbotMessageType.USER;

    @JoinColumn(name = "workspace_participant_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private WorkspaceParticipant workspaceParticipant;
}
