package com.Dolmeng_E.user.common.service;

import com.Dolmeng_E.user.common.dto.UserSignupEventDto;
import com.Dolmeng_E.user.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserSignupOrchestrationService {

    private final UserSignupProducer userSignupProducer;


    // 회원가입 이후 Kafka 이벤트 발행을 공통 처리

    public void publishSignupEvent(User user) {
        UserSignupEventDto event = UserSignupEventDto.builder()
                .userId(user.getId().toString())
                .userName(user.getName())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();

        userSignupProducer.publishUserSignup(event);
    }


}
