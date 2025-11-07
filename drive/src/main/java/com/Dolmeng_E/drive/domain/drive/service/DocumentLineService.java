package com.Dolmeng_E.drive.domain.drive.service;

import com.Dolmeng_E.drive.common.dto.EditorBatchMessageDto;
import com.Dolmeng_E.drive.domain.drive.dto.DocumentKafkaSaveDto;
import com.Dolmeng_E.drive.domain.drive.dto.DocumentKafkaUpdateDto;
import com.Dolmeng_E.drive.domain.drive.dto.DocumentLineResDto;
import com.Dolmeng_E.drive.domain.drive.dto.OnlineUserResDto;
import com.Dolmeng_E.drive.domain.drive.entity.Document;
import com.Dolmeng_E.drive.domain.drive.entity.DocumentLine;
import com.Dolmeng_E.drive.domain.drive.repository.DocumentLineRepository;
import com.Dolmeng_E.drive.domain.drive.repository.DocumentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class DocumentLineService {

    private final DocumentLineRepository documentLineRepository;
    private final DocumentRepository documentRepository;
    private final HashOperations<String, String, String> hashOperations;
    private final HashOperations<String, String, String> userHashOperations;
    private final SetOperations<String, String> setOperations;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public DocumentLineService(DocumentLineRepository documentLineRepository, DocumentRepository documentRepository, RedisTemplate<String, String> redisTemplate, @Qualifier("userInventory")RedisTemplate<String, String> userRedisTemplate, KafkaTemplate<String, String> kafkaTemplate) {
        this.documentLineRepository = documentLineRepository;
        this.documentRepository = documentRepository;
        this.hashOperations = redisTemplate.opsForHash();
        this.setOperations = redisTemplate.opsForSet();
        this.redisTemplate = redisTemplate;
        this.userHashOperations = userRedisTemplate.opsForHash();
        this.kafkaTemplate = kafkaTemplate;
    }


    // 문서에서 모든 라인 가져오기
    @Transactional(readOnly = true)
    public List<DocumentLineResDto> findAllDocumentLinesByDocumentId(String documentId) {
        // 1. 모든 라인을 DB에서 가져옵니다.
        List<DocumentLine> documentLines = documentLineRepository.findAllDocumentLinesByDocumentId(documentId);
        // 2. HashMap 준비 및 루트 노드 찾기 (반복문 한 번으로 통합)
        Map<String, DocumentLine> sequenceMap = new HashMap<>(); // key: prevId, value: 다음 DocumentLine
        DocumentLine rootLine = null;
        for (DocumentLine line : documentLines) {
            String prevId = line.getPrevId();
            if (prevId == null) {
                // 이 라인이 유일한 루트 라인입니다.
                rootLine = line;
            } else {
                // 이전 ID를 key로 하여 현재 라인을 value로 저장합니다.
                // 나중에 이전 라인을 가지고 다음 라인을 빠르게 찾기 위함입니다.
                sequenceMap.put(prevId, line);
            }
        }
        // 3. 루트부터 순서대로 최종 리스트 만들기
        List<DocumentLineResDto> sortedLineList = new ArrayList<>();
        //  루트 라인이 존재할 경우에만 순회 시작
        if (rootLine != null) {
            DocumentLine currentLine = rootLine;
            // currentLine이 null이 될 때까지 (즉, 마지막 라인까지) 반복합니다.
            while (currentLine != null) {
                // 현재 라인을 DTO로 변환하여 결과 리스트에 추가
                sortedLineList.add(DocumentLineResDto.builder()
                        .lineId(currentLine.getLineId())
                        .content(currentLine.getContent())
                        .id(currentLine.getId())
                        .prevId(currentLine.getPrevId() != null ? currentLine.getPrevId() : null)
                        .lockedBy(hashOperations.get("lock:"+documentId, currentLine.getLineId()))
                        .build());
                currentLine = sequenceMap.get(currentLine.getLineId());
            }
        }
        return sortedLineList;
    }

    @Transactional(readOnly = true)
    public List<OnlineUserResDto> findAllOnlineUsersByDocumentId(String documentId){
        List<OnlineUserResDto> onlineUserResDtoList = new ArrayList<>();
        Set<String> getOnlineUsers = setOperations.members("online:"+documentId);
        for (String user : getOnlineUsers) {
            Map<String, String> userInfo = userHashOperations.entries("user:"+user);
            onlineUserResDtoList.add(OnlineUserResDto.builder()
                    .userId(user)
                    .userName(userInfo.get("name"))
                    .profileImage(userInfo.get("profileImageUrl"))
                    .build());
        }
        return onlineUserResDtoList;
    }

    public void updateDocumentLine(String lineId, String content){
        DocumentLine documentLine = documentLineRepository.findByLineId(lineId)
                .orElseThrow(()->new EntityNotFoundException("해당 라인이 존재하지 않습니다." + lineId));
        documentLine.updateContent(content);

        // kafka 메시지 발행
        List<DocumentKafkaSaveDto.DocumentLineDto> documentLineDtos = new ArrayList<>();
        documentLineDtos.add(DocumentKafkaSaveDto.DocumentLineDto.builder()
                .id(documentLine.getId())
                .content(content)
                .type("UPDATE")
                .build());
        DocumentKafkaUpdateDto documentKafkaUpdateDto = DocumentKafkaUpdateDto.builder()
                .eventType("DOCUMENT_UPDATED")
                .eventPayload(DocumentKafkaUpdateDto.EventPayload.builder()
                        .id(documentLine.getDocument().getId())
                        .docLines(documentLineDtos)
                        .build())
                .build();
        try {
            // 3. DTO를 JSON 문자열로 변환
            String message = objectMapper.writeValueAsString(documentKafkaUpdateDto);

            // 4. Kafka 토픽으로 이벤트 발행
            kafkaTemplate.send("document-topic", message);

        } catch (JsonProcessingException e) {
            // 예외 처리 (심각한 경우 트랜잭션 롤백 고려)
            throw new RuntimeException("Kafka 메시지 직렬화 실패", e);
        }
    }

    public void createDocumentLine(String lineId, String prevId, String content, String documentId){
        Document document = documentRepository.findById(documentId)
                .orElseThrow(()->new EntityNotFoundException("해당 문서가 존재하지 않습니다."));

        // 만약 중간에 끼어들어갈 경우 순서 바꿔주기
        Optional<DocumentLine> documentLine = documentLineRepository.findByPrevId(prevId);
        documentLine.ifPresent(line -> line.updatePrevId(lineId));

        // 1. DTO를 Entity로 변환합니다.
        DocumentLine newDocumentLine = null;
        newDocumentLine = DocumentLine.builder()
                .prevId(prevId)
                .document(document)
                .lineId(lineId)
                .content(content)
                .build();

        // 2. Repository를 통해 데이터베이스에 저장합니다.
        documentLineRepository.saveAndFlush(newDocumentLine);

        // kafka 메시지 발행
        List<DocumentKafkaSaveDto.DocumentLineDto> documentLineDtos = new ArrayList<>();
        documentLineDtos.add(DocumentKafkaSaveDto.DocumentLineDto.builder()
                .id(newDocumentLine.getId())
                .content(content)
                .type("CREATE")
                .build());
        DocumentKafkaUpdateDto documentKafkaUpdateDto = DocumentKafkaUpdateDto.builder()
                .eventType("DOCUMENT_UPDATED")
                .eventPayload(DocumentKafkaUpdateDto.EventPayload.builder()
                        .id(newDocumentLine.getDocument().getId())
                        .docLines(documentLineDtos)
                        .build())
                .build();
        try {
            // 3. DTO를 JSON 문자열로 변환
            String message = objectMapper.writeValueAsString(documentKafkaUpdateDto);

            // 4. Kafka 토픽으로 이벤트 발행
            kafkaTemplate.send("document-topic", message);

        } catch (JsonProcessingException e) {
            // 예외 처리 (심각한 경우 트랜잭션 롤백 고려)
            throw new RuntimeException("Kafka 메시지 직렬화 실패", e);
        }
    }

    public void deleteDocumentLine(String lineId){
        DocumentLine reqDocumentLine = documentLineRepository.findByLineId(lineId).orElseThrow(()->new EntityNotFoundException(lineId));
        // 만약 뒷 라인이 있다면 앞단과 연결 시켜주기
        Optional<DocumentLine> documentLine = documentLineRepository.findByPrevId(lineId);
        documentLine.ifPresent(line -> line.updatePrevId(reqDocumentLine.getPrevId()));
        // 현재 라인 삭제
        documentLineRepository.delete(documentLineRepository.findByLineId(lineId).orElseThrow(()->new EntityNotFoundException("해당 라인이 존재하지 않습니다.")));

        // kafka 메시지 발행
        List<DocumentKafkaSaveDto.DocumentLineDto> documentLineDtos = new ArrayList<>();
        documentLineDtos.add(DocumentKafkaSaveDto.DocumentLineDto.builder()
                .id(reqDocumentLine.getId())
                .type("DELETE")
                .build());
        DocumentKafkaUpdateDto documentKafkaUpdateDto = DocumentKafkaUpdateDto.builder()
                .eventType("DOCUMENT_UPDATED")
                .eventPayload(DocumentKafkaUpdateDto.EventPayload.builder()
                        .id(reqDocumentLine.getDocument().getId())
                        .docLines(documentLineDtos)
                        .build())
                .build();
        try {
            // 3. DTO를 JSON 문자열로 변환
            String message = objectMapper.writeValueAsString(documentKafkaUpdateDto);

            // 4. Kafka 토픽으로 이벤트 발행
            kafkaTemplate.send("document-topic", message);

        } catch (JsonProcessingException e) {
            // 예외 처리 (심각한 경우 트랜잭션 롤백 고려)
            throw new RuntimeException("Kafka 메시지 직렬화 실패", e);
        }
    }

    public void batchUpdateDocumentLine(EditorBatchMessageDto messages){
        String documentId = messages.getDocumentId();
        String senderId = messages.getSenderId();
        for(EditorBatchMessageDto.Changes changes : messages.getChangesList()){
            if(changes.getType().equals("UPDATE")){
                updateDocumentLine(changes.getLineId(), changes.getContent());
            }else if(changes.getType().equals("DELETE")){
                deleteDocumentLine(changes.getLineId());
            }else if(changes.getType().equals("CREATE")){
                createDocumentLine(changes.getLineId(), changes.getPrevLineId(), changes.getContent(), documentId);
            }
        }
    }

    public Boolean LockDocumentLine(EditorBatchMessageDto message){
        String key = message.getDocumentId();
        Map<String, String> contentMap = null;
        try {
            contentMap = objectMapper.readValue(message.getContent(), Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        String lineId = contentMap.get("lineId");

        if (Boolean.TRUE.equals(hashOperations.putIfAbsent("lock:"+key, lineId, message.getSenderId()))) {
            setOperations.add("user_lock:"+key+message.getSenderId(), lineId);
            return true;
        } else {
            return false;
        }
    }

    public void unLockDocumentLine(EditorBatchMessageDto message){
        String key = message.getDocumentId();

        for(EditorBatchMessageDto.Changes changes : message.getChangesList()){
            hashOperations.delete("lock:" + key, changes.getLineId());
        }
        redisTemplate.delete("user_lock:" + key + message.getSenderId());
    }

    public void unLockDocumentLineByUser(String documentId, String userId){
        Set<String> getlineIds = setOperations.members("user_lock:"+documentId+userId);
        if(getlineIds == null){return ;}
        for(String lineId : getlineIds){
            hashOperations.delete("lock:"+documentId, lineId);
        }
        redisTemplate.delete("user_lock:"+documentId+userId);
    }

    public EditorBatchMessageDto joinUser(EditorBatchMessageDto message){
        setOperations.add("online:"+message.getDocumentId(), message.getSenderId());
        Map<String, String> userInfo = userHashOperations.entries("user:"+message.getSenderId());
        message.setSenderName(userInfo.get("name"));
        message.setProfileImage(userInfo.get("profileImageUrl"));
        return message;
    }

    public void leaveUser(String documentId, String userId){
        unLockDocumentLineByUser(documentId, userId);
        setOperations.remove("online:"+documentId, userId);
    }

    public OnlineUserResDto getUserInfo(String userId){
        Map<String, String> userInfo = userHashOperations.entries("user:"+userId);
        return OnlineUserResDto.builder()
                .userId(userId)
                .userName(userInfo.get("name"))
                .profileImage(userInfo.get("profileImageUrl"))
                .build();
    }
}