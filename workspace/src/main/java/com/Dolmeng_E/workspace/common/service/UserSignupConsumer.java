package com.Dolmeng_E.workspace.common.service;

import com.Dolmeng_E.workspace.common.dto.UserSignupEventDto;
import com.Dolmeng_E.workspace.common.dto.UserSignupFailEventDto;
import com.Dolmeng_E.workspace.domain.workspace.dto.PersonalWorkspaceCreateDto;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceTemplates;
import com.Dolmeng_E.workspace.domain.workspace.service.WorkspaceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSignupConsumer {

    private final WorkspaceService workspaceService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String USER_SIGNUP_FAIL_TOPIC = "user-signup-fail-topic";

    @Transactional
    @KafkaListener(topics = "user-signup-topic", groupId = "workspace-group")
    public void consume(UserSignupEventDto event) {
        try {
            log.info("회원가입 이벤트 수신: {}", event);

            try {
                PersonalWorkspaceCreateDto dto = PersonalWorkspaceCreateDto.builder()
                        .workspaceName(event.getUserName() + "의 워크스페이스")
                        .userId(UUID.fromString(event.getUserId()))
                        .workspaceTemplates(WorkspaceTemplates.PERSONAL)
                        .userName(event.getUserName())
                        .build();

                workspaceService.createPersonalWorkspace(dto);
                log.info("개인 워크스페이스 자동 생성 완료 for {}", event.getEmail());

            } catch (Exception inner) {
                // 서비스 내부에서 던진 예외를 잡음
                log.error("워크스페이스 생성 실패: {}", inner.getMessage());

                UserSignupFailEventDto failEvent = new UserSignupFailEventDto(event.getUserId());
                kafkaTemplate.send(USER_SIGNUP_FAIL_TOPIC, failEvent);
                log.warn("보상 트랜잭션 이벤트 발행 완료: {}", failEvent.getUserId());
            }

        } catch (Exception outer) {
            // 혹시라도 Kafka Listener 자체에서 던져진 예외를 잡음
            log.error("Kafka Listener 단계에서 발생한 예외: {}", outer.getMessage());
        }
    }

}
