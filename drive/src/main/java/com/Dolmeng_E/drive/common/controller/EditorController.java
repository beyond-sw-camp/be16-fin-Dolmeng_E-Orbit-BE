package com.Dolmeng_E.drive.common.controller;

import com.Dolmeng_E.drive.common.dto.EditorMessageDto;
import com.Dolmeng_E.drive.domain.drive.repository.DocumentLineRepository;
import com.Dolmeng_E.drive.domain.drive.repository.DocumentRepository;
import com.Dolmeng_E.drive.domain.drive.service.DocumentLineService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final DocumentLineRepository documentLineRepository;
    private final DocumentRepository documentRepository;
    private final DocumentLineService documentLineService;

    ObjectMapper objectMapper = new ObjectMapper();

    // 클라이언트가 /app/editor/update 경로로 메시지를 보내면 이 메서드가 처리
    @MessageMapping("/editor/update")
    public void handleUpdate(@Payload EditorMessageDto message) {
//        driverService.updateDocument(message.getDocumentId(), message.getContent());
        // 받은 메시지를 "document-updates" 채널로 발행(publish)
        documentLineService.updateDocumentLine(message);
        redisTemplate.convertAndSend("document-updates", message);
    }

    @MessageMapping("/editor/cursor")
    public void handleCursorUpdate(@Payload EditorMessageDto message) {
        // 커서 위치는 DB에 저장하지 않고 바로 브로드캐스트
        redisTemplate.convertAndSend("document-updates", message);
    }

    @MessageMapping("/editor/create")
    public void handleCreate(@Payload EditorMessageDto message) {
        documentLineService.createDocumentLine(message);
        // 3. (다음 단계) 웹소켓을 통해 다른 사용자들에게 이 블록 생성 정보를 브로드캐스트합니다.
        redisTemplate.convertAndSend("document-updates", message);
    }

    @MessageMapping("/editor/delete")
    public void handleDelete(@Payload EditorMessageDto message) {
        documentLineService.deleteDocumentLine(message);
        // 3. (다음 단계) 웹소켓을 통해 다른 사용자들에게 이 블록 생성 정보를 브로드캐스트합니다.
        redisTemplate.convertAndSend("document-updates", message);
    }
}