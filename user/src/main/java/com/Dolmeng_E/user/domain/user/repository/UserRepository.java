package com.Dolmeng_E.user.domain.user.repository;

import com.Dolmeng_E.user.domain.user.entity.User;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findBySocialId(String socialId);
    // 이메일, 이름 기준 부분검색
    List<User> findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(String emailKeyword, String nameKeyword);

    // 특정 ID 목록에 포함되지 않은 사용자 조회
    @Query("SELECT u FROM User u WHERE u.id NOT IN :excludedIds")
    List<User> findAllNotInIds(@Param("excludedIds") List<UUID> excludedIds);
}
