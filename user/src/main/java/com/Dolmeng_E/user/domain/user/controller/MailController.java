package com.Dolmeng_E.user.domain.user.controller;

import com.Dolmeng_E.user.domain.user.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MailController {

    private final MailService mailService;

    @GetMapping("/auth/email")
    public void sendMimeMessage() {
        mailService.sendMimeMessage();
    }
}