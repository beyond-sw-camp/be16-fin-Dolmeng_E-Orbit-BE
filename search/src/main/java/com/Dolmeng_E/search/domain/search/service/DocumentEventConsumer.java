package com.Dolmeng_E.search.domain.search.service;

import com.Dolmeng_E.search.domain.search.dto.EventDto;
import com.Dolmeng_E.search.domain.search.entity.DocumentDocument;
import com.Dolmeng_E.search.domain.search.entity.DocumentLine;
import com.Dolmeng_E.search.domain.search.repository.DocumentDocumentRepository;
import com.Dolmeng_E.search.domain.search.repository.FileDocumentRepository; // 불필요시 제거
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class DocumentEventConsumer {
    private final ObjectMapper objectMapper;
    private final HashOperations<String, String, String> hashOperations;
    private final DocumentDocumentRepository documentDocumentRepository;
    private final HtmlParsingService htmlParsingService;

    public DocumentEventConsumer(ObjectMapper objectMapper, RedisTemplate<String, String> redisTemplate, DocumentDocumentRepository documentDocumentRepository, FileDocumentRepository fileDocumentRepository, HtmlParsingService htmlParsingService) {
        this.objectMapper = objectMapper;
        this.hashOperations = redisTemplate.opsForHash();
        this.documentDocumentRepository = documentDocumentRepository;
        this.htmlParsingService = htmlParsingService;
    }


    @KafkaListener(topics = "document-topic", groupId = "search-consumer-group")
    public void handleDocument(String eventMessage, Acknowledgment ack) {
        try {
            EventDto eventDto = objectMapper.readValue(eventMessage, EventDto.class);
            String eventType = eventDto.getEventType();
            EventDto.EventPayload eventPayload = eventDto.getEventPayload();

            switch (eventType) {
                case "DOCUMENT_CREATED":
                    String key = "user:"+eventPayload.getCreatedBy();
                    Map<String, String> userInfo = hashOperations.entries(key);
                    List<DocumentLine> documentLines = new ArrayList<>();

                    DocumentDocument document = DocumentDocument.builder()
                            .id(eventPayload.getId())
                            .docType("DOCUMENT")
                            .searchTitle(eventPayload.getSearchTitle())
                            .docLines(documentLines)
                            .dateTime(eventPayload.getCreatedAt().toLocalDate())
                            .viewableUserIds(eventPayload.getViewableUserIds())
                            .createdBy(eventPayload.getCreatedBy())
                            .creatorName(userInfo.get("name"))
                            .profileImageUrl(userInfo.get("profileImageUrl"))
                            .rootId(eventPayload.getRootId())
                            .rootType(eventPayload.getRootType())
                            .parentId(eventPayload.getParentId())
                            .workspaceId(eventPayload.getWorkspaceId())
                            .build();

                    documentDocumentRepository.save(document);
                    System.out.println("ES 색인(C/U) 성공: " + document.getId());
                    ack.acknowledge();
                    break;

                case "DOCUMENT_UPDATED":
                    Optional<DocumentDocument> optionalDocument = documentDocumentRepository.findById(eventPayload.getId());
                    if (optionalDocument.isPresent()) {
                        DocumentDocument documentToUpdate = optionalDocument.get();

                        if(eventPayload.getSearchTitle()!=null){
                            documentToUpdate.updateDocument(eventPayload);
                        }
                        if (eventPayload.getDocLines() != null) {
                            List<DocumentLine> documentLineToUpdate =  documentToUpdate.getDocLines();
                            List<EventDto.DocumentLineDto> docLines = eventPayload.getDocLines();
                            for(EventDto.DocumentLineDto documentLineDto : docLines) {
                                // 생성
                                if(documentLineDto.getType().equals("CREATE")){
                                    documentLineToUpdate.add(DocumentLine.builder()
                                            .id(documentLineDto.getId())
                                            .content(htmlParsingService.extractText(documentLineDto.getContent()))
                                            .build());
                                // 수정
                                }else if(documentLineDto.getType().equals("UPDATE")){
                                    Optional<DocumentLine> found = documentLineToUpdate.stream()
                                            .filter(documentLine -> documentLine.getId().equals(documentLineDto.getId()))
                                            .findFirst();
                                    found.ifPresent(documentLine -> documentLine.setContent(htmlParsingService.extractText(documentLineDto.getContent())));
                                }else if(documentLineDto.getType().equals("DELETE")){
                                    Long targetId = documentLineDto.getId();
                                    documentLineToUpdate.removeIf(documentLine -> documentLine.getId().equals(targetId));
                                }
                            }
                            documentToUpdate.setDocLines(documentLineToUpdate);
                        }

                        // (기타 메타데이터 업데이트 로직 동일)
                        if(eventPayload.getRootId()!=null){
                            documentToUpdate.setRootId(eventPayload.getRootId());
                        }
                        if(eventPayload.getRootType()!=null){
                            documentToUpdate.setRootType(eventPayload.getRootType());
                        }
                        if (eventPayload.getViewableUserIds()!=null){
                            documentToUpdate.setViewableUserIds(eventPayload.getViewableUserIds());
                        }
                        documentToUpdate.setParentId(eventPayload.getParentId());

                        documentDocumentRepository.save(documentToUpdate);
                        System.out.println("ES 업데이트(U) 성공: " + documentToUpdate.getId());
                    } else {
                        System.err.println("ES 업데이트(U) 실패: 원본 문서를 찾을 수 없음 - ID: " + eventPayload.getId());
                    }
                    ack.acknowledge();
                    break;

                case "DOCUMENT_DELETED":
                    // (로직 동일)
                    String deletedDocumentId = eventPayload.getId();
                    documentDocumentRepository.deleteById(deletedDocumentId);
                    ack.acknowledge();
                    System.out.println("ES 삭제(D) 성공: " + deletedDocumentId);
                    break;

                default:
                    ack.acknowledge();
                    System.err.println("알 수 없는 이벤트 타입: " + eventType);
                    break;
            }

        } catch (Exception e) {
            System.err.println("ES 처리 실패: " + e.getMessage());
        }
    }
}