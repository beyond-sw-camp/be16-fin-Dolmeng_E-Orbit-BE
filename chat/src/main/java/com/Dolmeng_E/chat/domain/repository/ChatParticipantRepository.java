package com.Dolmeng_E.chat.domain.repository;

import com.Dolmeng_E.chat.domain.entity.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant,Long> {
}
