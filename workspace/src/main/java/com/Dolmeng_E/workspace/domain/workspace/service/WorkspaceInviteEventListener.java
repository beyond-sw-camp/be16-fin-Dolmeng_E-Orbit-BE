package com.Dolmeng_E.workspace.domain.workspace.service;


import com.Dolmeng_E.workspace.domain.workspace.dto.WorkspaceInviteEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class WorkspaceInviteEventListener {

    private final JavaMailSender mailSender;

    // 트랜잭션 커밋 후에만 실행됨
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleInviteEvent(WorkspaceInviteEvent event) {

        String inviteLink = "http://localhost:5173/invite/accept?token=" + event.getToken();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(event.getEmail());
        message.setSubject("[Dolmeng_E] 워크스페이스 초대: " + event.getWorkspaceName());
        message.setText("""
            안녕하세요!

            %s 워크스페이스로 초대되었습니다.
            아래 링크를 클릭해 참여를 완료해주세요 👇

            %s

            초대코드는 24시간 동안 유효합니다.
            """.formatted(event.getWorkspaceName(), inviteLink));

        mailSender.send(message);
    }

}