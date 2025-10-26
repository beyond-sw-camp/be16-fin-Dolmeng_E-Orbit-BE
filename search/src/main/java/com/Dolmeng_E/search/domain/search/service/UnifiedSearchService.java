package com.Dolmeng_E.search.domain.search.service;

import com.Dolmeng_E.search.domain.search.dto.DocumentResDto;
import com.Dolmeng_E.search.domain.search.entity.UnifiedSearchDocument;
import com.Dolmeng_E.search.domain.search.repository.UnifiedSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class UnifiedSearchService {
    private final UnifiedSearchRepository unifiedSearchRepository;

    // 이름을 통한 검색
    public List<DocumentResDto> searchDocumentByTitle(String title) {
        List<UnifiedSearchDocument> unifiedSearchDocuments = unifiedSearchRepository.findBySearchTitle(title);
        List<DocumentResDto> documentResDtos = new ArrayList<>();
        for (UnifiedSearchDocument unifiedSearchDocument : unifiedSearchDocuments) {
            DocumentResDto documentResDto = DocumentResDto.builder()
                    .searchTitle(unifiedSearchDocument.getSearchTitle())
                    .build();
            documentResDtos.add(documentResDto);
        }
        return documentResDtos;
    }
}
