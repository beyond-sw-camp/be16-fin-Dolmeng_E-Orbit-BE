package com.Dolmeng_E.search.domain.search.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import com.Dolmeng_E.search.domain.search.dto.DocumentSearchResDto;
import com.Dolmeng_E.search.domain.search.dto.DocumentSuggestResDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
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
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UnifiedSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    private final IndexCoordinates ALL_INDICES =
            IndexCoordinates.of("stones", "documents", "files", "tasks");

    /**
     * ✅ 통합 검색 수행 (ignoreUnmapped(true) 옵션으로 수정)
     */
    public List<DocumentSearchResDto> search(String keyword, String currentUserId, String workspaceId) {

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
                                                        // 2-1. searchTitle.ngram
                                                        .match(mt -> mt
                                                                .field("searchTitle.ngram")
                                                                .query(keyword)
                                                        )
                                                )
                                                .should(s -> s
                                                        // 2-2. searchContent (matchPhrase)
                                                        .matchPhrase(mp -> mp
                                                                .field("searchContent")
                                                                .query(keyword)
                                                                .analyzer("nori")
                                                        )
                                                )
                                                // ▼▼▼ [수정] bool/exists 래퍼 제거, ignoreUnmapped(true) 추가 ▼▼▼
                                                .should(s -> s
                                                        .nested(n -> n
                                                                .path("docLines")
                                                                .query(nq -> nq
                                                                        .matchPhrase(nm -> nm
                                                                                .field("docLines.content")
                                                                                .query(keyword)
                                                                                .analyzer("nori")
                                                                        )
                                                                )
                                                                .innerHits(ih -> ih
                                                                        .name("docLines")
                                                                        .highlight(h -> h
                                                                                .fields("docLines.content", f -> f
                                                                                        .preTags(List.of("<em>"))
                                                                                        .postTags(List.of("</em>"))
                                                                                )
                                                                        )
                                                                        .size(3)
                                                                )
                                                                // [핵심] docLines 필드가 없는 인덱스에서 이 쿼리를 무시합니다.
                                                                .ignoreUnmapped(true)
                                                        )
                                                )
                                                // ▲▲▲ [수정] 끝 ▲▲▲
                                                .minimumShouldMatch("1")
                                        )
                                )
                                // 3. 사용자 필터
                                .filter(f -> f
                                        .term(t -> t
                                                .field("viewableUserIds")
                                                .value(currentUserId)
                                        )
                                )
                                // 4. 워크스페이스 ID 필터
                                .filter(f -> f
                                        .term(t -> t
                                                .field("workspaceId")
                                                .value(workspaceId)
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

        // ✅ 4. 결과 처리 (헬퍼 메소드 사용 - 변경 없음)
        return searchHits.stream()
                .map(this::mapHitToDto)
                .collect(Collectors.toList());
    }

    /**
     * ✅ 통합 검색어 자동완성 (변경 없음)
     */
    public List<DocumentSuggestResDto> suggest(String keyword, String currentUserId, String workspaceId) {

        String suggestField = "searchTitle.ngram";
        Pageable pageable = PageRequest.of(0, 10);

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> b
                                .must(m -> m
                                        .matchBoolPrefix(mbp -> mbp
                                                .field(suggestField)
                                                .query(keyword)
                                                .analyzer("nori_search_analyzer")
                                        )
                                )
                                .filter(f -> f
                                        .term(t -> t
                                                .field("viewableUserIds")
                                                .value(currentUserId)
                                        )
                                )
                                .filter(f -> f
                                        .term(t -> t
                                                .field("workspaceId")
                                                .value(workspaceId)
                                        )
                                )
                        )
                )
                .withPageable(pageable)
                .withSourceFilter(new FetchSourceFilter(
                        new String[]{"searchTitle", "docType", "fileUrl"}, null))
                .build();

        SearchHits<DocumentSuggestResDto> searchHits = elasticsearchOperations.search(
                query,
                DocumentSuggestResDto.class,
                ALL_INDICES
        );

        return searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    /**
     * ✅ highlight, innerHits 안전 처리를 위한 헬퍼 메소드 (변경 없음)
     */
    private DocumentSearchResDto mapHitToDto(SearchHit<DocumentSearchResDto> hit) {
        DocumentSearchResDto dto = hit.getContent();
        Map<String, List<String>> highlights = hit.getHighlightFields();

        // 제목 highlight
        if (highlights.containsKey("searchTitle")) {
            dto.setSearchTitle(String.join("", highlights.get("searchTitle")));
        }

        // 본문 highlight (전역 하이라이트 먼저 확인)
        if (highlights.containsKey("searchContent")) {
            dto.setSearchContent(String.join("", highlights.get("searchContent")));

            // 본문 하이라이트가 없으면 inner_hits (docLines) 확인
        } else if (hit.getInnerHits() != null && hit.getInnerHits().containsKey("docLines")) {
            List<String> docLineHighlights = hit.getInnerHits().get("docLines").stream()
                    .flatMap(innerHit -> {
                        Map<String, List<String>> innerHighlights = innerHit.getHighlightFields();
                        if (innerHighlights == null) return Stream.empty();
                        return innerHighlights.getOrDefault("docLines.content", List.of()).stream();
                    })
                    .collect(Collectors.toList());

            if (!docLineHighlights.isEmpty()) {
                // 여러 줄을 줄바꿈(\n)으로 연결
                dto.setSearchContent(String.join("...", docLineHighlights));
            }

            // 하이라이트가 아무것도 없으면 원본 내용 자르기
        } else if (dto.getSearchContent() != null) {
            dto.setSearchContent(
                    dto.getSearchContent().length() > 200
                            ? dto.getSearchContent().substring(0, 200) + "..."
                            : dto.getSearchContent()
            );
        }

        return dto;
    }
}