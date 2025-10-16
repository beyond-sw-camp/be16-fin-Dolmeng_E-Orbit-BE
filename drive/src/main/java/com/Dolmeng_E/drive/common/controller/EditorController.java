package com.Dolmeng_E.drive.common.controller;

import com.Dolmeng_E.drive.common.dto.EditorMessageDto;
import com.Dolmeng_E.drive.domain.drive.service.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class EditorController {

    // Redis에 메시지를 발행하기 위한 RedisTemplate
    private final RedisTemplate<String, Object> redisTemplate;

    // 클라이언트가 /app/editor/update 경로로 메시지를 보내면 이 메서드가 처리
    @MessageMapping("/editor/update")
    public void handleUpdate(@Payload EditorMessageDto message) {
//        driverService.updateDocument(message.getDocumentId(), message.getContent());
        // 받은 메시지를 "document-updates" 채널로 발행(publish)
        redisTemplate.opsForValue().set(message.getDocumentId(), message.getContent());
        redisTemplate.convertAndSend("document-updates", message);
    }

    @MessageMapping("/editor/cursor")
    public void handleCursorUpdate(@Payload EditorMessageDto message) {
        // 커서 위치는 DB에 저장하지 않고 바로 브로드캐스트
        redisTemplate.convertAndSend("document-updates", message);
    }
}