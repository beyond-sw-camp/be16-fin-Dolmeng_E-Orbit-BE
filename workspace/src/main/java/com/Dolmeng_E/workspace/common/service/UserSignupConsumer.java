package com.Dolmeng_E.workspace.common.service;

import com.Dolmeng_E.workspace.common.dto.UserSignupEventDto;
import com.Dolmeng_E.workspace.common.dto.UserSignupFailEventDto;
import com.Dolmeng_E.workspace.domain.workspace.dto.PersonalWorkspaceCreateDto;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceTemplates;
import com.Dolmeng_E.workspace.domain.workspace.service.WorkspaceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
public class UserSignupConsumer {

    private final WorkspaceService workspaceService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String USER_SIGNUP_FAIL_TOPIC = "user-signup-fail-topic";

    public UserSignupConsumer(
            WorkspaceService workspaceService,
            @Qualifier("compensationKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate
    ) {
        this.workspaceService = workspaceService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    @KafkaListener(topics = "user-signup-topic", groupId = "workspace-group")
    public void consume(UserSignupEventDto event) {
        try {
            log.info("회원가입 이벤트 수신: {}", event);

            PersonalWorkspaceCreateDto dto = PersonalWorkspaceCreateDto.builder()
                    .workspaceName(event.getUserName() + "의 워크스페이스")
                    .userId(UUID.fromString(event.getUserId()))
                    .workspaceTemplates(WorkspaceTemplates.PERSONAL)
                    .userName(event.getUserName())
                    .build();

            workspaceService.createPersonalWorkspace(dto);
            log.info("개인 워크스페이스 자동 생성 완료 for {}", event.getEmail());

        } catch (Exception e) {
            log.error("워크스페이스 생성 실패: {}", e.getMessage());

            UserSignupFailEventDto failEvent = new UserSignupFailEventDto(event.getUserId());
            kafkaTemplate.send(USER_SIGNUP_FAIL_TOPIC, failEvent);
            log.warn("보상 트랜잭션 이벤트 발행 완료: {}", failEvent.getUserId());
        }
    }
}


