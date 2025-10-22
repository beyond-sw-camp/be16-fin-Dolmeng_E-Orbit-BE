package com.Dolmeng_E.user.common.service;

import com.Dolmeng_E.user.common.dto.UserSignupEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserSignupProducer {

    private static final String TOPIC = "user-signup-topic"; // 발행할 토픽명
    private final KafkaTemplate<String, UserSignupEventDto> kafkaTemplate;

    public void publishUserSignup(UserSignupEventDto event) {
        kafkaTemplate.send(TOPIC, event);
        System.out.println("Kafka 메시지 발행 완료: " + event.getUserId());
    }
}