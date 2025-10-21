package com.Dolmeng_E.workspace.common.service;

import com.Dolmeng_E.workspace.common.dto.UserSignupEventDto;
import com.Dolmeng_E.workspace.common.dto.UserSignupFailEventDto;
import com.Dolmeng_E.workspace.domain.workspace.dto.PersonalWorkspaceCreateDto;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceTemplates;
import com.Dolmeng_E.workspace.domain.workspace.service.WorkspaceService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSignupConsumer {

    private final WorkspaceService workspaceService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String USER_SIGNUP_FAIL_TOPIC = "user-signup-fail-topic";

    @KafkaListener(topics = "user-signup-topic", groupId = "workspace-group")
    public void consume(UserSignupEventDto event) {
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

        } catch (Exception e) {
            log.error("워크스페이스 자동 생성 실패 for {}: {}", event.getEmail(), e.getMessage());

            // 보상 트랜잭션 이벤트 발행
            UserSignupFailEventDto failEvent = new UserSignupFailEventDto(event.getUserId());
            kafkaTemplate.send(USER_SIGNUP_FAIL_TOPIC, failEvent);

            log.warn("보상 트랜잭션 이벤트 발행 완료: {}", failEvent.getUserId());
        }
    }
}
