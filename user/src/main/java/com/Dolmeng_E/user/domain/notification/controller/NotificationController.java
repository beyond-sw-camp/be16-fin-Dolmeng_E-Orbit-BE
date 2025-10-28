package com.Dolmeng_E.user.domain.notification.controller;

import com.Dolmeng_E.user.domain.notification.dto.NotificationCreateReqDto;
import com.Dolmeng_E.user.domain.notification.service.NotificationService;
import com.example.modulecommon.dto.CommonSuccessDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notification")
public class NotificationController {
    private final NotificationService notificationService;

    // 알림 생성
    @PostMapping("/new-noti")
    public ResponseEntity<?> createNotification(@RequestBody NotificationCreateReqDto dto) {
        notificationService.createNotification(dto);
        return new ResponseEntity<>(new CommonSuccessDto("CREATED", HttpStatus.CREATED.value(), "알림 생성 성공"), HttpStatus.CREATED);
    }

}
