package com.Dolmeng_E.chat.domain.repository;

import com.Dolmeng_E.chat.domain.entity.ReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReadStatusRepository extends JpaRepository<ReadStatus,Long> {
    // 특정 사용자가 특정 채팅방에서 읽지 않은 메시지 개수 조회
    Long countByUserIdAndChatRoom_IdAndIsReadFalse(UUID userId, Long chatRoomId);
}
