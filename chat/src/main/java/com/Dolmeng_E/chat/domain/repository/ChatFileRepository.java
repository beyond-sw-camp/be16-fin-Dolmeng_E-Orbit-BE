package com.Dolmeng_E.chat.domain.repository;

import com.Dolmeng_E.chat.domain.entity.ChatFile;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatFileRepository extends JpaRepository<ChatFile, Long> {
    @Query("""
        SELECT f 
        FROM ChatFile f
        JOIN f.chatMessage m
        JOIN m.chatRoom r
        WHERE r.id = :roomId
    """)
    List<ChatFile> findAllByRoomId(@Param("roomId") Long roomId);
}
