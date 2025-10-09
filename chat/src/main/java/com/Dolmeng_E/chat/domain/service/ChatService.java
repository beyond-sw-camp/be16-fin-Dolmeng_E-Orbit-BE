package com.Dolmeng_E.chat.domain.service;

import com.Dolmeng_E.chat.domain.repository.ChatParticipantRepository;
import com.Dolmeng_E.chat.domain.repository.ChatRoomRepository;
import com.Dolmeng_E.chat.domain.repository.MessageReadStatusRepository;
import com.Dolmeng_E.chat.domain.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatService {
    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final MessageReadStatusRepository messageReadStatusRepository;
}
