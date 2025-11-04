package com.Dolmeng_E.search.domain.search.service;

import com.Dolmeng_E.search.domain.search.dto.StoneEventDto;
import com.Dolmeng_E.search.domain.search.entity.ParticipantInfo;
import com.Dolmeng_E.search.domain.search.entity.StoneDocument;
import com.Dolmeng_E.search.domain.search.repository.StoneDocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class StoneEventConsumer {
    private final ObjectMapper objectMapper; // JSON 파싱용
    private final HashOperations<String, String, String> hashOperations; // 유저 정보 가져오는 용도
    private final StoneDocumentRepository stoneDocumentRepository;

    public StoneEventConsumer(ObjectMapper objectMapper, RedisTemplate<String, String> redisTemplate, StoneDocumentRepository stoneDocumentRepository) {
        this.objectMapper = objectMapper;
        this.hashOperations = redisTemplate.opsForHash();
        this.stoneDocumentRepository = stoneDocumentRepository;
    }

    @KafkaListener(topics = "stone-topic", groupId = "search-consumer-group")
    public void handleFile(String eventMessage, Acknowledgment ack) {
        try {
            // 1. Kafka 메시지(JSON)를 DTO로 파싱
            StoneEventDto stoneEventDto = objectMapper.readValue(eventMessage, StoneEventDto.class);
            String eventType = stoneEventDto.getEventType();
            StoneEventDto.EventPayload eventPayload = stoneEventDto.getEventPayload();

            // 2. eventType에 따라 분기 처리
            switch (eventType) {
                case "STONE_CREATED":
                    // 생성
                    String key = "user:"+eventPayload.getManager();
                    Map<String, String> userInfo = hashOperations.entries(key);
                    List<StoneEventDto.EventPayload.ParticipantInfo> participantInfos = eventPayload.getParticipants();
                    List<ParticipantInfo> participantInfoList = new ArrayList<>();
                    for(StoneEventDto.EventPayload.ParticipantInfo participantInfo : participantInfos) {
                        Map<String, String> participant = hashOperations.entries("user:"+participantInfo.getId());
                        participantInfoList.add(ParticipantInfo.builder()
                                .id(participantInfo.getId())
                                .name(participant.get("name"))
                                .profileImageUrl(participant.get("profileImageUrl"))
                                .build());
                    }
                    StoneDocument stoneDocument = StoneDocument.builder()
                            .id(eventPayload.getId())
                            .docType("STONE")
                            .searchTitle(eventPayload.getName())
                            .searchContent(eventPayload.getDescription())
                            .dateTime(eventPayload.getEndDate().toLocalDate())
                            .viewableUserIds(eventPayload.getViewableUserIds())
                            .createdBy(eventPayload.getManager())
                            .creatorName(userInfo.get("name"))
                            .profileImageUrl(userInfo.get("profileImageUrl"))
                            .rootId(eventPayload.getProjectId())
                            .rootType("PROJECT")
                            .stoneStatus(eventPayload.getStatus())
                            .participantInfos(participantInfoList)
                            .workspaceId(eventPayload.getWorkspaceId())
                            .build();
                    stoneDocumentRepository.save(stoneDocument); // ES에 저장 또는 업데이트
                    System.out.println("ES 색인(C/U) 성공: " + stoneDocument.getId());
                    ack.acknowledge();
                    break;
                case "STONE_UPDATED":
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
