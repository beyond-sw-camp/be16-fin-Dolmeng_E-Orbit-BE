package com.Dolmeng_E.user.domain.user.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender javaMailSender;

    public String sendMimeMessage(String fromUser) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        String authCode = createAuthCode();

        try{
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            // 메일을 받을 수신자 설정
            mimeMessageHelper.setTo(fromUser);
            // 메일의 제목 설정
            mimeMessageHelper.setSubject("Orbit : 인증코드 안내");

            // html 문법 적용한 메일의 내용
            String content = String.format("""
                <!DOCTYPE html>
                <html>
                <body>
                    <div style="margin:50px; font-family: Arial, sans-serif; color:#2A2828;">
                        <table width="100%%" cellpadding="20" cellspacing="0" style="border-collapse:collapse; text-align:center;">
                            <tr style="background-color:#FFE364;">
                                <td style="font-size:24px; font-weight:bold; color:#2A2828;">
                                    Orbit 이메일 인증
                                </td>
                            </tr>
                            <tr>
                                <td style="font-size:16px;">
                                    아래 코드를 기입하여 인증을 완료해주세요.
                                </td>
                            </tr>
                            <tr>
                                <td style="font-size:20px; font-weight:bold; color:green;">
                                    인증번호: %s
                                </td>
                            </tr>
                        </table>
                    </div>
                </body>
                </html>
                """, authCode);


            // 메일의 내용 설정
            mimeMessageHelper.setText(content, true);

            javaMailSender.send(mimeMessage);

            log.info("메일 발송 성공!");

            return authCode;
        } catch (Exception e) {
            log.info("메일 발송 실패!");
            throw new RuntimeException(e);
        }
    }

    private String createAuthCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(10)); // 0~9 중 하나 뽑기
        }

        return sb.toString();
    }
}