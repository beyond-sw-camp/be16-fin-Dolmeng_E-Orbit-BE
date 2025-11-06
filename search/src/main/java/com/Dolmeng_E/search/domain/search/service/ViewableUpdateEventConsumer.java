package com.Dolmeng_E.search.domain.search.service;

import com.Dolmeng_E.search.domain.search.dto.StoneEventDto;
import com.Dolmeng_E.search.domain.search.dto.StoneViewableUpdateEventDto;
import com.Dolmeng_E.search.domain.search.entity.*;
import com.Dolmeng_E.search.domain.search.repository.DocumentDocumentRepository;
import com.Dolmeng_E.search.domain.search.repository.FileDocumentRepository;
import com.Dolmeng_E.search.domain.search.repository.StoneDocumentRepository;
import com.Dolmeng_E.search.domain.search.repository.TaskDocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ViewableUpdateEventConsumer {
    private final ObjectMapper objectMapper; // JSON 파싱용
    private final HashOperations<String, String, String> hashOperations; // 유저 정보 가져오는 용도
    private final StoneDocumentRepository stoneDocumentRepository;
    private final TaskDocumentRepository taskDocumentRepository;
    private final FileDocumentRepository fileDocumentRepository;
    private final DocumentDocumentRepository documentDocumentRepository;

    public ViewableUpdateEventConsumer(ObjectMapper objectMapper, RedisTemplate<String, String> redisTemplate, StoneDocumentRepository stoneDocumentRepository, TaskDocumentRepository taskDocumentRepository, FileDocumentRepository fileDocumentRepository, DocumentDocumentRepository documentDocumentRepository) {
        this.objectMapper = objectMapper;
        this.hashOperations = redisTemplate.opsForHash();
        this.stoneDocumentRepository = stoneDocumentRepository;
        this.taskDocumentRepository = taskDocumentRepository;
        this.fileDocumentRepository = fileDocumentRepository;
        this.documentDocumentRepository = documentDocumentRepository;
    }

    @KafkaListener(topics = "update-viewable-topic", groupId = "search-consumer-group")
    public void handleFile(String eventMessage, Acknowledgment ack) {
        try {
            // 1. Kafka 메시지(JSON)를 DTO로 파싱
            StoneViewableUpdateEventDto viewableUpdateEventDto = objectMapper.readValue(eventMessage, StoneViewableUpdateEventDto.class);
            String eventType = viewableUpdateEventDto.getEventType();
            StoneViewableUpdateEventDto.EventPayload eventPayload = viewableUpdateEventDto.getEventPayload();

            // 2. eventType에 따라 분기 처리
            switch (eventType) {
                case "STONE_PARTICIPANT_UPDATE":
                    StoneDocument stoneDocument = stoneDocumentRepository.findById(eventPayload.getId()).orElseThrow();
                    List<TaskDocument> taskDocuments = taskDocumentRepository.findAllByRootId(eventPayload.getId());
                    List<FileDocument> stoneFileDocuments = fileDocumentRepository.findAllByRootId(eventPayload.getId());
                    List<FileDocument> projectFileDocuments = fileDocumentRepository.findAllByRootId(eventPayload.getProjectId());
                    List<DocumentDocument> stoneDocumentDocuments = documentDocumentRepository.findAllByRootId(eventPayload.getId());
                    List<DocumentDocument> projectDocumentDocuments = documentDocumentRepository.findAllByRootId(eventPayload.getProjectId());

                    Set<String> participantIds = eventPayload.getUserIds().stream()
                            .map(UUID::toString) // 각 UUID 객체를 .toString()을 호출해 문자열로 변환
                            .collect(Collectors.toSet());
                    List<ParticipantInfo> participantInfos = stoneDocument.getParticipantInfos();
                    // 스톤 참여자 추가일 경우
                    if(eventPayload.getType().equals("CREATE")){
                        // 스톤 참여자, 권한 추가
                        Set<String> viewableUserIds = stoneDocument.getViewableUserIds();
                        for(String participantId : participantIds){
                            Map<String, String> userInfo = hashOperations.entries("user:"+participantId.toString());
                            participantInfos.add(ParticipantInfo.builder()
                                    .id(participantId)
                                    .name(userInfo.get("name"))
                                    .profileImageUrl(userInfo.get("profileImageUrl"))
                                    .build());
                            viewableUserIds.add(participantId);
                        }
                        // 각 요소 추가
                        taskDocuments.forEach(doc -> {
                            doc.getViewableUserIds().addAll(participantIds);
                        });
                        stoneFileDocuments.forEach(doc -> {
                            doc.getViewableUserIds().addAll(participantIds);
                        });
                        projectFileDocuments.forEach(doc -> {
                            doc.getViewableUserIds().addAll(participantIds);
                        });
                        stoneDocumentDocuments.forEach(doc -> {
                            doc.getViewableUserIds().addAll(participantIds);
                        });
                        projectDocumentDocuments.forEach(doc -> {
                            doc.getViewableUserIds().addAll(participantIds);
                        });

                    }else if(eventPayload.getType().equals("DELETE")){
                        for(String participantId : participantIds){
                            participantInfos.removeIf(participant -> participant.getId().equals(participantId));
                            Set<String> viewableUserIds = stoneDocument.getViewableUserIds();
                            viewableUserIds.remove(participantId);
                        }

                        // 각 요소 제거 - 프로젝트 제외
                        taskDocuments.forEach(doc -> {
                            doc.getViewableUserIds().removeAll(participantIds);
                        });
                        stoneFileDocuments.forEach(doc -> {
                            doc.getViewableUserIds().removeAll(participantIds);
                        });
                        stoneDocumentDocuments.forEach(doc -> {
                            doc.getViewableUserIds().removeAll(participantIds);
                        });
                    }

                    stoneDocumentRepository.save(stoneDocument); // 또는 업데이트
                    taskDocumentRepository.saveAll(taskDocuments);
                    fileDocumentRepository.saveAll(stoneFileDocuments);
                    documentDocumentRepository.saveAll(stoneDocumentDocuments);
                    fileDocumentRepository.saveAll(projectFileDocuments);
                    documentDocumentRepository.saveAll(projectDocumentDocuments);

                    System.out.println("ES 색인(C/U) 성공: " + stoneDocument.getId());
                    ack.acknowledge();
                    break;
                case "WORKSPACE_PARTICIPANT_UPDATE":
                    Set<String> workspaceParticipantIds = eventPayload.getUserIds().stream()
                            .map(UUID::toString) // 각 UUID 객체를 .toString()을 호출해 문자열로 변환
                            .collect(Collectors.toSet());
                    List<DocumentDocument> workspaceDocumentDocuments = documentDocumentRepository.findAllByRootId(eventPayload.getId());
                    List<FileDocument> workspaceFileDocuments = fileDocumentRepository.findAllByRootId(eventPayload.getId());
                    workspaceFileDocuments.forEach(doc -> {
                        doc.getViewableUserIds().addAll(workspaceParticipantIds);
                    });
                    workspaceDocumentDocuments.forEach(doc -> {
                        doc.getViewableUserIds().addAll(workspaceParticipantIds);
                    });
                    documentDocumentRepository.saveAll(workspaceDocumentDocuments);
                    fileDocumentRepository.saveAll(workspaceFileDocuments);
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
