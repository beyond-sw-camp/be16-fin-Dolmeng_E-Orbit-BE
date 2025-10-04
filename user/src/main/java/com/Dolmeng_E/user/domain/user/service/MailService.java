package com.Dolmeng_E.user.domain.user.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender javaMailSender;

    public void sendMimeMessage() {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        try{
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            // 메일을 받을 수신자 설정
            mimeMessageHelper.setTo("dmsqls6789@naver.com");
            // 메일의 제목 설정
            mimeMessageHelper.setSubject("Orbit : 인증코드 안내");

            int authCode = 123456;
            // html 문법 적용한 메일의 내용
            String content = """
            <!DOCTYPE html>
            <html xmlns:th="http://www.thymeleaf.org">
            <body>
                <div style="margin:50px; font-family: Arial, sans-serif; color:#2A2828;">
                    <table width="100%" cellpadding="20" cellspacing="0" style="border-collapse:collapse; text-align:center;">
                        
                        <!-- 첫 번째 행: 헤더 -->
                        <tr style="background-color:#FFE364;">
                            <td style="font-size:24px; font-weight:bold; color:#2A2828;">
                                Orbit 이메일 인증
                            </td>
                        </tr>
                        
                        <!-- 두 번째 행: 안내 문구 -->
                        <tr>
                            <td style="font-size:16px;">
                                아래 코드를 기입하여 인증을 완료해주세요.
                            </td>
                        </tr>
                        
                        <!-- 세 번째 행: 인증번호 -->
                        <tr>
                            <td style="font-size:20px; font-weight:bold; color:green;">
                                인증번호: <span th:text="${authCode}">123456</span>
                            </td>
                        </tr>
                    </table>
                </div>
            </body>
            </html>
            """;
            
            // 메일의 내용 설정
            mimeMessageHelper.setText(content, true);

            javaMailSender.send(mimeMessage);

            log.info("메일 발송 성공!");
        } catch (Exception e) {
            log.info("메일 발송 실패!");
            throw new RuntimeException(e);
        }
    }
}