package com.Dolmeng_E.chat_db.domain.repository;

import com.Dolmeng_E.chat_db.domain.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Query("""
        SELECT DISTINCT cr
        FROM ChatRoom cr
        JOIN cr.chatParticipantList cp
        WHERE cp.userId = :userId
          AND cr.isDelete = 'N'
          AND cr.workspaceId = :workspaceId
    """)
    List<ChatRoom> findAllByUserAndWorkspace(@Param("userId") UUID userId,
                                             @Param("workspaceId") String workspaceId);
}
