package com.Dolmeng_E.drive.common.controller;

import com.Dolmeng_E.drive.common.dto.EditorBatchMessageDto;
import com.Dolmeng_E.drive.domain.drive.service.DocumentLineService;
import com.Dolmeng_E.drive.domain.drive.service.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.Set;

@Controller
@RequiredArgsConstructor
public class EditorController {

    // Redis에 메시지를 발행하기 위한 RedisTemplate
    private final RedisTemplate<String, Object> redisTemplate;
    private final DocumentLineService documentLineService;

    @MessageMapping("/editor/cursor")
    public void handleCursorUpdate(@Payload EditorBatchMessageDto message) {
        // 커서 위치는 DB에 저장하지 않고 바로 브로드캐스트
        redisTemplate.convertAndSend("document-updates", message);
    }

    @MessageMapping("/editor/batch-update")
    public void handleBatchUpdate(@Payload EditorBatchMessageDto message) {
        documentLineService.batchUpdateDocumentLine(message);
        redisTemplate.convertAndSend("document-updates", message);
    }

    @MessageMapping("/editor/lock-line")
    public void handleLock(@Payload EditorBatchMessageDto message) {
        if(documentLineService.LockDocumentLine(message)){
            redisTemplate.convertAndSend("document-updates", message);
        };
    }

    @MessageMapping("/editor/unlock-line")
    public void handleUnLock(@Payload EditorBatchMessageDto message) {
        documentLineService.unLockDocumentLine(message);
        redisTemplate.convertAndSend("document-updates", message);
    }

    @MessageMapping("/editor/leave")
    public void handleLeaveUser(@Payload EditorBatchMessageDto message) {
        documentLineService.leaveUser(message.getDocumentId(), message.getSenderId());
        redisTemplate.convertAndSend("document-updates", message);
    }

    @MessageMapping("/editor/join")
    public void handleJoinUser(@Payload EditorBatchMessageDto message) {
        documentLineService.joinUser(message.getDocumentId(), message.getSenderId());
        redisTemplate.convertAndSend("document-updates", message);
    }
}