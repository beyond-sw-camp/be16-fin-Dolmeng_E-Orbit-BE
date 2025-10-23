package com.Dolmeng_E.chat_db.domain.repository;

import com.Dolmeng_E.chat_db.domain.entity.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant,Long> {
    List<ChatParticipant> findByChatRoomId(Long chatRoomId);
}
