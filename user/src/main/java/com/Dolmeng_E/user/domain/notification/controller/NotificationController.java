package com.Dolmeng_E.user.domain.notification.controller;

import com.Dolmeng_E.user.domain.notification.dto.NotificationCreateReqDto;
import com.Dolmeng_E.user.domain.notification.dto.NotificationListResDto;
import com.Dolmeng_E.user.domain.notification.entity.NotificationType;
import com.Dolmeng_E.user.domain.notification.service.NotificationKafkaService;
import com.Dolmeng_E.user.domain.notification.service.NotificationService;
import com.example.modulecommon.dto.CommonSuccessDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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

    // 알림 목록 조회
    @GetMapping("/list")
    public ResponseEntity<?> getNotifications(@RequestHeader("X-User-Id") String userId) {
        List<NotificationListResDto> notificationListResDto = notificationService.getNotifications(userId);
        return new ResponseEntity<>(new CommonSuccessDto(notificationListResDto, HttpStatus.OK.value(), "알림 목록 조회 성공"),  HttpStatus.OK);
    }

    // 알림 읽음 처리
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<?> readNotification(@PathVariable Long notificationId) {
        notificationService.readNotification(notificationId);
        return new ResponseEntity<>(new CommonSuccessDto(notificationId, HttpStatus.OK.value(), "알림 읽음 처리 성공"),  HttpStatus.OK);
    }

    // 알림 삭제 처리
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<?> removeNotification(@PathVariable Long notificationId) {
        notificationService.removeNotification(notificationId);
        return new ResponseEntity<>(new CommonSuccessDto(notificationId, HttpStatus.OK.value(), "알림 삭제 처리 성공"),  HttpStatus.OK);
    }

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        List<UUID> userIdList = new ArrayList<>();
        userIdList.add(UUID.fromString("59c720ae-8c62-41d0-abcb-a247113ba2e9"));

        NotificationCreateReqDto dto1 = NotificationCreateReqDto.builder()
                .title("test111")
                .content("test222")
                .userIdList(userIdList)
                .type("PROJECT_CONFIRMED")
                .sendAt(LocalDateTime.now().plusSeconds(10))
                .build();

        notificationKafkaService.kafkaNotificationPublish(dto1);

        NotificationCreateReqDto dto2 = NotificationCreateReqDto.builder()
                .title("test333")
                .content("test444")
                .userIdList(userIdList)
                .type("PROJECT_CONFIRMED")
                .sendAt(LocalDateTime.now().plusSeconds(20))
                .build();

        notificationKafkaService.kafkaNotificationPublish(dto2);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
