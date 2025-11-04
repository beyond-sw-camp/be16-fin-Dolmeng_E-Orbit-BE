package com.Dolmeng_E.search.domain.search.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import com.Dolmeng_E.search.domain.search.dto.DocumentSearchResDto;
import com.Dolmeng_E.search.domain.search.dto.DocumentSuggestResDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UnifiedSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    private final IndexCoordinates ALL_INDICES =
            IndexCoordinates.of("stones", "documents", "files", "tasks");

    /**
     * ✅ 통합 검색 수행 (변경 없음)
     * (검색: searchTitle.ngram (N-gram) OR searchContent (Nori))
     */
    public List<DocumentSearchResDto> search(String keyword, String currentUserId) {

        Pageable pageable = PageRequest.of(0, 20);

        // 1. HighlightParameters 생성
        HighlightParameters highlightParameters = HighlightParameters.builder()
                .withPreTags("<em>")
                .withPostTags("</em>")
                .build();

        // 2. HighlightField 목록 생성
        List<HighlightField> highlightFields = List.of(
                new HighlightField("searchTitle"),
                new HighlightField("searchContent")
        );

        // 3. Highlight 객체 생성
        Highlight highlight = new Highlight(highlightParameters, highlightFields);


        // ✅ HighlightQuery 생성
        HighlightQuery highlightQuery = new HighlightQuery(highlight, DocumentSearchResDto.class);

        // ✅ 2. NativeQuery
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> b
                                .must(m -> m
                                        .bool(bShould -> bShould
                                                .should(s -> s
                                                        // 2-1. searchTitle.ngram (변경 없음)
                                                        .match(mt -> mt
                                                                .field("searchTitle.ngram")
                                                                .query(keyword)
                                                        )
                                                )
                                                // ▼▼▼ [수정] match -> matchPhrase ▼▼▼
                                                .should(s -> s
                                                        // 2-2. searchContent (Nori + "구문 검색"으로 변경)
                                                        .matchPhrase(mp -> mp // .match 대신 .matchPhrase
                                                                        .field("searchContent")
                                                                        .query(keyword)
                                                                        .analyzer("nori")
                                                                // .operator(Operator.And) // <-- 구문 검색에서는 제거
                                                        )
                                                )
                                                // ▲▲▲ [수정] 끝 ▲▲▲
                                                .minimumShouldMatch("1")
                                        )
                                )
                                // 3. 사용자 필터 (변경 없음)
                                .filter(f -> f
                                        .term(t -> t
                                                .field("viewableUserIds")
                                                .value(currentUserId)
                                        )
                                )
                        )
                )
                .withHighlightQuery(highlightQuery)
                .withPageable(pageable)
                .build();

        // ✅ 3. 검색 실행
        SearchHits<DocumentSearchResDto> searchHits = elasticsearchOperations.search(
                query,
                DocumentSearchResDto.class,
                ALL_INDICES
        );

        // ✅ 4. 결과 처리
        return searchHits.stream()
                .map(hit -> {
                    DocumentSearchResDto dto = hit.getContent();
                    Map<String, List<String>> highlights = hit.getHighlightFields();

                    if (highlights.containsKey("searchTitle")) {
                        dto.setSearchTitle(String.join("", highlights.get("searchTitle")));
                    }

                    if (highlights.containsKey("searchContent")) {
                        dto.setSearchContent(String.join("", highlights.get("searchContent")));
                    } else if (dto.getSearchContent() != null) {
                        dto.setSearchContent(
                                dto.getSearchContent().length() > 200
                                        ? dto.getSearchContent().substring(0, 200) + "..."
                                        : dto.getSearchContent()
                        );
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * ✅ 통합 검색어 자동완성 (수정됨: DocumentSuggestResDto 객체 리스트 반환)
     * (자동완성: searchTitle.ngram (N-gram) - 제목으로만 검색)
     */
    public List<DocumentSuggestResDto> suggest(String keyword, String currentUserId) {

        String suggestField = "searchTitle.ngram";
        Pageable pageable = PageRequest.of(0, 10);

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> b
                                // 1. [수정됨] 제목(searchTitle.ngram)으로만 검색 (이전 대화 반영)
                                .must(m -> m
                                        .matchBoolPrefix(mbp -> mbp
                                                .field(suggestField)
                                                .query(keyword)
                                                .analyzer("nori_search_analyzer")
                                        )
                                )
                                // 2. 사용자 필터 조건 (변경 없음)
                                .filter(f -> f
                                        .term(t -> t
                                                .field("viewableUserIds")
                                                .value(currentUserId)
                                        )
                                )
                        )
                )
                .withPageable(pageable)
                // [수정] DTO 필드에 맞게 _source 필터링
                // (id는 metadata, searchTitle/docType은 _source에서 가져옴)
                .withSourceFilter(new FetchSourceFilter(
                        new String[]{"searchTitle", "docType", "fileUrl"}, null))
                .build();

        // [수정 1] DTO 클래스 변경
        SearchHits<DocumentSuggestResDto> searchHits = elasticsearchOperations.search(
                query,
                DocumentSuggestResDto.class, // <-- 반환 DTO 클래스 지정
                ALL_INDICES
        );

        // [수정 2] DTO 객체 자체를 리스트로 변환
        return searchHits.stream()
                .map(hit -> hit.getContent()) // <-- DTO 객체 추출
                // .distinct()는 제거 (DTO 객체는 ID별로 고유하므로)
                .collect(Collectors.toList());
    }
}