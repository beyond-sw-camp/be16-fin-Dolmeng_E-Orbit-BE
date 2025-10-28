package com.Dolmeng_E.search.domain.search.service;

import com.Dolmeng_E.search.domain.search.dto.EventDto;
import com.Dolmeng_E.search.domain.search.entity.DocumentDocument;
import com.Dolmeng_E.search.domain.search.repository.DocumentDocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class EventConsumer {
    private final ObjectMapper objectMapper; // JSON 파싱용
    private final HashOperations<String, String, String> hashOperations;
    private final DocumentDocumentRepository documentDocumentRepository;

    public EventConsumer(ObjectMapper objectMapper, RedisTemplate<String, String> redisTemplate, DocumentDocumentRepository documentDocumentRepository) {
        this.objectMapper = objectMapper;
        this.hashOperations = redisTemplate.opsForHash();
        this.documentDocumentRepository = documentDocumentRepository;
    }

    @KafkaListener(topics = "document-topic", groupId = "search-consumer-group")
    public void handleDocument(String eventMessage, Acknowledgment ack) {
        try {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            // 1. Kafka 메시지(JSON)를 DTO로 파싱
            EventDto eventDto = objectMapper.readValue(eventMessage, EventDto.class);
            String eventType = eventDto.getEventType();
            EventDto.EventPayload eventPayload = eventDto.getEventPayload();

            // 2. eventType에 따라 분기 처리
            switch (eventType) {
                case "DOCUMENT_CREATED":
                case "DOCUMENT_UPDATED":
                    // 생성과 수정은 ES에서 동일하게 save()를 사용 (Upsert: 없으면 생성, 있으면 덮어쓰기)
                    String key = "user:"+eventPayload.getCreatedBy();
                    Map<String, String> userInfo = hashOperations.entries(key);
                    DocumentDocument document = DocumentDocument.builder()
                            .id(eventPayload.getId())
                            .docType("DOCUMENT")
                            .searchTitle(eventPayload.getSearchTitle())
                            .searchContent(eventPayload.getSearchContent())
                            .dateTime(eventPayload.getCreatedAt().toLocalDate())
                            .viewableUserIds(eventPayload.getViewableUserIds())
                            .createdBy(eventPayload.getCreatedBy())
                            .creatorName(userInfo.get("name"))
                            .profileImageUrl(userInfo.get("profileImageUrl"))
                            .build();
                    documentDocumentRepository.save(document); // ES에 저장 또는 업데이트
                    System.out.println("ES 색인(C/U) 성공: " + document.getId());
                    ack.acknowledge();
                    break;

                case "DOCUMENT_DELETED":
                    // 삭제는 ID만 파싱
                    String deletedDocumentId = eventPayload.getId();

                    documentDocumentRepository.deleteById(deletedDocumentId); // ES에서 삭제
                    ack.acknowledge();
                    System.out.println("ES 삭제(D) 성공: " + deletedDocumentId);
                    break;

                default:
                    // 알 수 없는 이벤트 타입 로그 처리
                    ack.acknowledge();
                    System.err.println("알 수 없는 이벤트 타입: " + eventType);
                    break;
            }

        } catch (Exception e) {
            System.err.println("ES 처리 실패: " + e.getMessage());
        }
    }
}
