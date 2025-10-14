package com.Dolmeng_E.chat.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ChatMedia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String mediaUrl;

    @JoinColumn(name = "chat_message")
    @ManyToOne(fetch = FetchType.LAZY)
    private ChatMessage chatMessage;
}

// http요청으로 파일 저장하고, ChatMedia객체 리스트를 반환
// 반환 받으면 그제서야 ws로 기존 메시지랑, 파일 url ws로 전송
// ChatMessageDto를 바꿔줘야 할듯 - 메세지 타입이랑, 파일 링크 리스트