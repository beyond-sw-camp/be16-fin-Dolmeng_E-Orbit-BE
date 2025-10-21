package com.Dolmeng_E.user.common.service;

import com.Dolmeng_E.user.common.dto.UserSignupFailEventDto;
import com.Dolmeng_E.user.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserSignupFailListener {

    private final UserRepository userRepository;

    @KafkaListener(topics = "user-signup-fail-topic", groupId = "user-compensation-group")
    public void handleSignupFail(UserSignupFailEventDto event) {
        userRepository.findById(UUID.fromString(event.getUserId())).ifPresent(user -> {
            userRepository.delete(user);
            System.err.println("보상 트랜잭션 실행: 사용자 삭제 완료 → " + event.getUserId());
        });
    }
}