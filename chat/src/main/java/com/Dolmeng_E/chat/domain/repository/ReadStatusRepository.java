package com.Dolmeng_E.chat.domain.repository;

import com.Dolmeng_E.chat.domain.entity.ReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReadStatusRepository extends JpaRepository<ReadStatus,Long> {
}
