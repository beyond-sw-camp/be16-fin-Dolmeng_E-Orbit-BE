package com.Dolmeng_E.workspace.domain.workspace.service;

import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    public void sendInviteMail(String to, String inviteCode, String workspaceName, boolean isExistingUser) {
        String subject = "[Workspace 초대] " + workspaceName + "에 참여하세요!";
        String link = "https://yourapp.com/workspaces/invite/accept?code=" + inviteCode;

        String body;
        if (isExistingUser) {
            body = """
                    안녕하세요!
                    
                   
                    아래 링크를 클릭하면 바로 워크스페이스에 참여하실 수 있습니다.
                    
                    ▶ 참여하기: %s
                    
                    - 워크스페이스 이름: %s
                    """.formatted(link, workspaceName);
        } else {
            body = """
                    안녕하세요!
                    
                    %s 워크스페이스에 초대되었습니다.
                    아직 회원가입이 되어 있지 않아요.
                    
                    아래 링크에서 가입하신 후 자동으로 워크스페이스에 참여됩니다.
                    
                    ▶ 가입 및 참여하기: %s
                    
                    - 워크스페이스 이름: %s
                    """.formatted(workspaceName, link, workspaceName);
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("초대 메일 전송 완료 -> {}", to);
        } catch (Exception e) {
            log.error("초대 메일 전송 실패 -> {}", to, e);
        }
    }
}
