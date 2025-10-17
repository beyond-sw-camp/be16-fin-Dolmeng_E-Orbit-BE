package com.Dolmeng_E.drive.domain.drive.service;

import com.Dolmeng_E.drive.common.dto.EditorMessageDto;
import com.Dolmeng_E.drive.domain.drive.dto.DocumentLineResDto;
import com.Dolmeng_E.drive.domain.drive.entity.Document;
import com.Dolmeng_E.drive.domain.drive.entity.DocumentLine;
import com.Dolmeng_E.drive.domain.drive.repository.DocumentLineRepository;
import com.Dolmeng_E.drive.domain.drive.repository.DocumentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class DocumentLineService {
    private final DocumentLineRepository documentLineRepository;
    private final DocumentRepository documentRepository;

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
                        .build());
                currentLine = sequenceMap.get(currentLine.getLineId());
            }
        }
        return sortedLineList;
    }

    public void updateDocumentLine(EditorMessageDto message){
        DocumentLine documentLine = documentLineRepository.findByLineId(message.getLineId())
                .orElseThrow(()->new EntityNotFoundException("해당 라인이 존재하지 않습니다." + message.getLineId()));
        documentLine.updateContent(message.getContent());
    }

    public void createDocumentLine(EditorMessageDto message){
        Document document = documentRepository.findById(message.getDocumentId())
                .orElseThrow(()->new EntityNotFoundException("해당 문서가 존재하지 않습니다."));

        // 만약 중간에 끼어들어갈 경우 순서 바꿔주기
        Optional<DocumentLine> documentLine = documentLineRepository.findByPrevId(message.getPrevLineId());
        documentLine.ifPresent(line -> line.updatePrevId(message.getLineId()));

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
    }

    public void deleteDocumentLine(EditorMessageDto message){
        // 만약 뒷 라인이 있다면 앞단과 연결 시켜주기
        Optional<DocumentLine> documentLine = documentLineRepository.findByPrevId(message.getLineId());
        System.out.println(message.getPrevLineId());
        documentLine.ifPresent(line -> line.updatePrevId(message.getPrevLineId()));
        // 현재 라인 삭제
        documentLineRepository.delete(documentLineRepository.findByLineId(message.getLineId()).orElseThrow(()->new EntityNotFoundException("해당 라인이 존재하지 않습니다.")));
    }
}
