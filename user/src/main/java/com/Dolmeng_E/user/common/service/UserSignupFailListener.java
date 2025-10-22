package com.Dolmeng_E.user.common.service;

import com.Dolmeng_E.user.common.dto.UserSignupFailEventDto;
import com.Dolmeng_E.user.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSignupFailListener {

    private final UserRepository userRepository;

    @Transactional
    @KafkaListener(topics = "user-signup-fail-topic", groupId = "user-group")
    public void handleSignupFail(UserSignupFailEventDto failEvent) {
        String rawUserId = failEvent.getUserId();

        try {
            // 문자열을 여기서만 UUID로 변환
            UUID userId = UUID.fromString(rawUserId);
            log.info("[보상트랜잭션] 삭제 시도 → {}", userId);
            userRepository.deleteById(userId);
            log.info("[보상트랜잭션] 삭제 직후 존재 여부: {}", userRepository.findById(userId).isPresent());
            log.warn("보상 트랜잭션 실행: 사용자 삭제 완료 → {}", userId);

        } catch (IllegalArgumentException e) {
            log.error("보상 트랜잭션 실패 (UUID 파싱 실패): {}", rawUserId);
        } catch (Exception e) {
            log.error("보상 트랜잭션 처리 중 오류 발생: {}", e.getMessage());
        }
    }
}
