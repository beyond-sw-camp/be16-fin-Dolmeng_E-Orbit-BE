package com.Dolmeng_E.search.domain.search.service;

import com.Dolmeng_E.search.domain.search.dto.EventDto;
import com.Dolmeng_E.search.domain.search.entity.DocumentDocument;
import com.Dolmeng_E.search.domain.search.entity.FileDocument;
import com.Dolmeng_E.search.domain.search.repository.DocumentDocumentRepository;
import com.Dolmeng_E.search.domain.search.repository.FileDocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class FileEventConsumer {
    private final ObjectMapper objectMapper; // JSON 파싱용
    private final HashOperations<String, String, String> hashOperations; // 유저 정보 가져오는 용도
    private final FileDocumentRepository fileDocumentRepository;

    public FileEventConsumer(ObjectMapper objectMapper, RedisTemplate<String, String> redisTemplate, DocumentDocumentRepository documentDocumentRepository, FileDocumentRepository fileDocumentRepository) {
        this.objectMapper = objectMapper;
        this.hashOperations = redisTemplate.opsForHash();
        this.fileDocumentRepository = fileDocumentRepository;
    }

    @KafkaListener(topics = "file-topic", groupId = "search-consumer-group")
    public void handleFile(String eventMessage, Acknowledgment ack) {
        try {
            // 1. Kafka 메시지(JSON)를 DTO로 파싱
            EventDto eventDto = objectMapper.readValue(eventMessage, EventDto.class);
            String eventType = eventDto.getEventType();
            EventDto.EventPayload eventPayload = eventDto.getEventPayload();

            // 2. eventType에 따라 분기 처리
            switch (eventType) {
                case "FILE_CREATED":
                    // 생성
                    String key = "user:"+eventPayload.getCreatedBy();
                    Map<String, String> userInfo = hashOperations.entries(key);
                    FileDocument fileDocument = FileDocument.builder()
                            .id(eventPayload.getId())
                            .docType("FILE")
                            .searchTitle(eventPayload.getSearchTitle())
                            .searchContent(eventPayload.getSearchContent())
                            .dateTime(eventPayload.getCreatedAt().toLocalDate())
                            .viewableUserIds(eventPayload.getViewableUserIds())
                            .createdBy(eventPayload.getCreatedBy())
                            .creatorName(userInfo.get("name"))
                            .profileImageUrl(userInfo.get("profileImageUrl"))
                            .rootId(eventPayload.getRootId())
                            .rootType(eventPayload.getRootType())
                            .parentId(eventPayload.getParentId())
                            .fileUrl(eventPayload.getFileUrl())
                            .size(eventPayload.getSize())
                            .build();
                    fileDocumentRepository.save(fileDocument); // ES에 저장 또는 업데이트
                    System.out.println("ES 색인(C/U) 성공: " + fileDocument.getId());
                    ack.acknowledge();
                    break;
                case "FILE_UPDATED":
                    Optional<FileDocument> optionalFile = fileDocumentRepository.findById(eventPayload.getId());
                    if (optionalFile.isPresent()) {
                        FileDocument documentToUpdate = optionalFile.get();
                        if(eventPayload.getSearchTitle()!=null){
                            documentToUpdate.updateFile(eventPayload);
                        }
                        if(eventPayload.getRootId()!=null){
                            documentToUpdate.setRootId(eventPayload.getRootId());
                        }
                        if(eventPayload.getRootType()!=null){
                            documentToUpdate.setRootType(eventPayload.getRootType());
                        }
                        documentToUpdate.setParentId(eventPayload.getParentId());
                        fileDocumentRepository.save(documentToUpdate);
                        System.out.println("ES 업데이트(U) 성공: " + documentToUpdate.getId());
                    } else {
                        System.err.println("ES 업데이트(U) 실패: 원본 문서를 찾을 수 없음 - ID: " + eventPayload.getId());
                    }
                    ack.acknowledge();
                    break;

                case "FILE_DELETED":
                    // 삭제는 ID만 파싱
                    String deletedFileId = eventPayload.getId();

                    fileDocumentRepository.deleteById(deletedFileId); // ES에서 삭제
                    ack.acknowledge();
                    System.out.println("ES 삭제(D) 성공: " + deletedFileId);
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
