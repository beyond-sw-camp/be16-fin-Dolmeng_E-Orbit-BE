package com.Dolmeng_E.user.domain.notification.controller;

import com.Dolmeng_E.user.domain.notification.dto.NotificationCreateReqDto;
import com.Dolmeng_E.user.domain.notification.entity.NotificationType;
import com.Dolmeng_E.user.domain.notification.service.NotificationKafkaService;
import com.Dolmeng_E.user.domain.notification.service.NotificationService;
import com.example.modulecommon.dto.CommonSuccessDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notification")
public class NotificationController {
    private final NotificationService notificationService;
    private final NotificationKafkaService notificationKafkaService;

    // 알림 생성
    @PostMapping("/new-noti")
    public ResponseEntity<?> createNotification(@RequestBody NotificationCreateReqDto dto) {
        notificationService.createNotification(dto);
        return new ResponseEntity<>(new CommonSuccessDto("CREATED", HttpStatus.CREATED.value(), "알림 생성 성공"), HttpStatus.CREATED);
    }

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        List<UUID> userIdList = new ArrayList<>();
        userIdList.add(UUID.fromString("59c720ae-8c62-41d0-abcb-a247113ba2e9"));

        NotificationCreateReqDto dto = NotificationCreateReqDto.builder()
                .title("test111")
                .content("test222")
                .userIdList(userIdList)
                .type(NotificationType.PROJECT_CONFIRMED)
                .build();

        notificationKafkaService.kafkaNotificationPublish(dto);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
