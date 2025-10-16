package com.Dolmeng_E.drive.common.controller;

import com.Dolmeng_E.drive.common.dto.EditorMessageDto;
import com.Dolmeng_E.drive.domain.drive.entity.Document;
import com.Dolmeng_E.drive.domain.drive.entity.DocumentLine;
import com.Dolmeng_E.drive.domain.drive.repository.DocumentLineRepository;
import com.Dolmeng_E.drive.domain.drive.repository.DocumentRepository;
import com.Dolmeng_E.drive.domain.drive.service.DriverService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
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

    ObjectMapper objectMapper = new ObjectMapper();

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

    @MessageMapping("/editor/create")
    public void handleCreate(@Payload EditorMessageDto message) {
        Document document = documentRepository.findById(message.getDocumentId())
                .orElseThrow(()->new EntityNotFoundException("해당 문서가 존재하지 않습니다."));
        // 1. DTO를 Entity로 변환합니다.
        DocumentLine newDocumentLine = null;
        newDocumentLine = DocumentLine.builder()
                .prevId(message.getPrevLineId())
                .document(document)
                .lineId(message.getLineId())
                .content(message.getContent())
                .build();

        // 2. Repository를 통해 데이터베이스에 저장합니다.
        documentLineRepository.save(newDocumentLine);

        // 3. (다음 단계) 웹소켓을 통해 다른 사용자들에게 이 블록 생성 정보를 브로드캐스트합니다.
        // webSocketService.broadcastBlockCreation(newBlock);
    }
}