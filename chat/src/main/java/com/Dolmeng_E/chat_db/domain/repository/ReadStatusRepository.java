package com.Dolmeng_E.chat_db.domain.repository;

import com.Dolmeng_E.chat_db.domain.entity.ChatMessage;
import com.Dolmeng_E.chat_db.domain.entity.ChatRoom;
import com.Dolmeng_E.chat_db.domain.entity.ReadStatus;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReadStatusRepository extends JpaRepository<ReadStatus,Long> {
    // 특정 사용자가 특정 채팅방에서 읽지 않은 메시지 개수 조회
    Long countByUserIdAndChatRoom_IdAndIsReadFalse(UUID userId, Long chatRoomId);

    @Query("""
    SELECT rs.chatMessage
    FROM ReadStatus rs
    WHERE rs.chatRoom = :chatRoom
      AND rs.userId = :userId
      AND rs.isRead = false
    ORDER BY rs.chatMessage.id ASC
""")
    List<ChatMessage> findUnreadMessagesByChatRoomAndUserId(
            @Param("chatRoom") ChatRoom chatRoom,
            @Param("userId") UUID userId
    );

    @Query("""
    SELECT COUNT(rs)
    FROM ReadStatus rs
    JOIN rs.chatRoom cr
    WHERE cr.workspaceId = :workspaceId
      AND rs.userId = :userId
      AND rs.isRead = false
      AND cr.id IN (
          SELECT cp.chatRoom.id
          FROM ChatParticipant cp
          WHERE cp.userId = :userId
      )
    """)
    Long countUnreadMessagesInWorkspaceByUser(
            @Param("workspaceId") String workspaceId,
            @Param("userId") UUID userId
    );


}
