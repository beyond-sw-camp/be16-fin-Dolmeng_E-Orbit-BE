package com.Dolmeng_E.chat.domain.repository;

import com.Dolmeng_E.chat.domain.entity.ChatMessage;
import com.Dolmeng_E.chat.domain.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage,Long> {
    List<ChatMessage> findByChatRoomOrderByCreatedAtAsc(ChatRoom chatRoom);

}
