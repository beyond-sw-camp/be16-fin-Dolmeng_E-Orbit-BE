package com.Dolmeng_E.chat.domain.repository;

import com.Dolmeng_E.chat.domain.entity.MessageReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageReadStatusRepository extends JpaRepository<MessageReadStatus,Long> {
}
