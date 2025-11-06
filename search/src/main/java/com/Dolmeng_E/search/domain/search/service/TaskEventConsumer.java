package com.Dolmeng_E.search.domain.search.service;

import com.Dolmeng_E.search.domain.search.dto.TaskEventDto;
import com.Dolmeng_E.search.domain.search.entity.DocumentDocument;
import com.Dolmeng_E.search.domain.search.entity.StoneDocument;
import com.Dolmeng_E.search.domain.search.entity.TaskDocument;
import com.Dolmeng_E.search.domain.search.repository.StoneDocumentRepository;
import com.Dolmeng_E.search.domain.search.repository.TaskDocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Optional;

@Component
public class TaskEventConsumer {
    private final ObjectMapper objectMapper; // JSON 파싱용
    private final HashOperations<String, String, String> hashOperations; // 유저 정보 가져오는 용도
    private final StoneDocumentRepository stoneDocumentRepository;
    private final TaskDocumentRepository taskDocumentRepository;

    public TaskEventConsumer(ObjectMapper objectMapper, RedisTemplate<String, String> redisTemplate, StoneDocumentRepository stoneDocumentRepository, TaskDocumentRepository taskDocumentRepository) {
        this.objectMapper = objectMapper;
        this.hashOperations = redisTemplate.opsForHash();
        this.stoneDocumentRepository = stoneDocumentRepository;
        this.taskDocumentRepository = taskDocumentRepository;
    }

    @KafkaListener(topics = "task-topic", groupId = "search-consumer-group")
    public void handleFile(String eventMessage, Acknowledgment ack) {
        try {
            // 1. Kafka 메시지(JSON)를 DTO로 파싱
            TaskEventDto taskEventDto = objectMapper.readValue(eventMessage, TaskEventDto.class);
            String eventType = taskEventDto.getEventType();
            TaskEventDto.EventPayload eventPayload = taskEventDto.getEventPayload();

            // 2. eventType에 따라 분기 처리
            switch (eventType) {
                case "TASK_CREATED":
                    // 생성
                    String key = "user:"+eventPayload.getManager();
                    Map<String, String> userInfo = hashOperations.entries(key);
                    StoneDocument stoneDocument = stoneDocumentRepository.findById(eventPayload.getStoneId()).orElseThrow(() -> new RuntimeException("Stone not found"));
                    TaskDocument taskDocument = TaskDocument.builder()
                            .id(eventPayload.getId())
                            .docType("TASK")
                            .searchTitle(eventPayload.getName())
                            .dateTime(eventPayload.getEndDate().toLocalDate())
                            .viewableUserIds(eventPayload.getViewableUserIds())
                            .createdBy(eventPayload.getManager())
                            .creatorName(userInfo.get("name"))
                            .profileImageUrl(userInfo.get("profileImageUrl"))
                            .rootId(eventPayload.getStoneId())
                            .rootType("STONE")
                            .isDone(Boolean.valueOf(eventPayload.getStatus()))
                            .workspaceId(eventPayload.getWorkspaceId())
                            .projectId(stoneDocument.getRootId())
                            .build();
                    taskDocumentRepository.save(taskDocument); // ES에 저장 또는 업데이트
                    System.out.println("ES 색인(C/U) 성공: " + taskDocument.getId());
                    ack.acknowledge();
                    break;
                case "TASK_UPDATED":
                    Optional<TaskDocument> optionalTaskDocument = taskDocumentRepository.findById(eventPayload.getId());
                    if (optionalTaskDocument.isPresent()) {
                        TaskDocument taskToUpdate = optionalTaskDocument.get();
                        if(eventPayload.getName()!=null){
                            taskToUpdate.setSearchTitle(eventPayload.getName());
                        }
                        if(eventPayload.getManager()!=null){
                            taskToUpdate.setCreatedBy(eventPayload.getManager());
                        }
                        if(eventPayload.getEndDate()!=null){
                            taskToUpdate.setDateTime(eventPayload.getEndDate().toLocalDate());
                        }
                        if(eventPayload.getStatus()!=null){
                            taskToUpdate.setIsDone(Boolean.valueOf(eventPayload.getStatus()));
                        }
                        taskDocumentRepository.save(taskToUpdate);
                        System.out.println("ES 업데이트(U) 성공: " + taskToUpdate.getId());
                    } else {
                        System.err.println("ES 업데이트(U) 실패: 원본 문서를 찾을 수 없음 - ID: " + eventPayload.getId());
                    }
                    ack.acknowledge();
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
