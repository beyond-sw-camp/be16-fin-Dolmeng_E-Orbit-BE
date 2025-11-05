package com.Dolmeng_E.search.domain.search.service;

// ▼▼▼ [수정] co.elastic.clients.elasticsearch._types.query_dsl.Query 임포트 추가 ▼▼▼
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
// ▲▲▲ [수정] 끝 ▲▲▲
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
     * ✅ 통합 검색 수행 (workspaceId 필터 추가됨)
     */
    public List<DocumentSearchResDto> search(String keyword, String currentUserId, String workspaceId) {

        Pageable pageable = PageRequest.of(0, 20);

        // 1. HighlightParameters 생성 (변경 없음)
        HighlightParameters highlightParameters = HighlightParameters.builder()
                .withPreTags("<em>")
                .withPostTags("</em>")
                .build();

        // ▼▼▼ [수정] docLines.content 하이라이트 필드 추가 ▼▼▼
        List<HighlightField> highlightFields = List.of(
                new HighlightField("searchTitle"),
                new HighlightField("searchContent"),    // 파일/스톤용
                new HighlightField("docLines.content") // [추가] 문서 라인용
        );
        // ▲▲▲ [수정] 끝 ▲▲▲

        // 3. Highlight 객체 생성 (변경 없음)
        Highlight highlight = new Highlight(highlightParameters, highlightFields);


        // ✅ HighlightQuery 생성 (변경 없음)
        HighlightQuery highlightQuery = new HighlightQuery(highlight, DocumentSearchResDto.class);

        // --- ▼▼▼ [수정] 쿼리 정의 분리 및 nested 쿼리 추가 ▼▼▼ ---

        // 쿼리 1: searchTitle.ngram (공통 제목)
        Query titleQuery = Query.of(q -> q
                .match(mt -> mt
                        .field("searchTitle.ngram")
                        .query(keyword)
                )
        );

        // 쿼리 2: searchContent (파일/스톤 내용)
        Query contentQuery = Query.of(q -> q
                .matchPhrase(mp -> mp
                        .field("searchContent")
                        .query(keyword)
                        .analyzer("nori") // (설정한 default_nori_analyzer 이름으로 변경 가능)
                )
        );

        // 쿼리 3: docLines.content (문서 라인 내용 - Nested)
        Query nestedLinesQuery = Query.of(q -> q
                .nested(n -> n
                                .path("docLines")
                                .query(nq -> nq
                                        .match(m -> m
                                                .field("docLines.content")
                                                .query(keyword)
                                                // Kafka에서 저장 시 사용한 분석기와 동일하게 지정
                                                .analyzer("html_strip_nori_analyzer")
                                        )
                                )
                        // inner_hits는 필요 없다고 하셔서 제거
                )
        );

        // ✅ 2. NativeQuery (3개 쿼리를 bool.should로 통합)
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> b
                                .must(m -> m
                                        .bool(bShould -> bShould
                                                .should(titleQuery)       // 1. 제목
                                                .should(contentQuery)     // 2. 파일/스톤 내용
                                                .should(nestedLinesQuery) // 3. [추가] 문서 라인 내용
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
                                // 4. 워크스페이스 ID 필터 (변경 없음)
                                .filter(f -> f
                                        .term(t -> t
                                                .field("workspaceId") // Elasticsearch의 필드명
                                                .value(workspaceId)
                                        )
                                )
                        )
                )
                // --- ▲▲▲ [수정] 끝 ▲▲▲ ---
                .withHighlightQuery(highlightQuery)
                .withPageable(pageable)
                .build();

        // ✅ 3. 검색 실행 (변경 없음)
        SearchHits<DocumentSearchResDto> searchHits = elasticsearchOperations.search(
                query,
                DocumentSearchResDto.class,
                ALL_INDICES
        );

        // ✅ 4. 결과 처리 (하이라이트 로직 수정)
        return searchHits.stream()
                .map(hit -> {
                    DocumentSearchResDto dto = hit.getContent();
                    Map<String, List<String>> highlights = hit.getHighlightFields();

                    if (highlights.containsKey("searchTitle")) {
                        dto.setSearchTitle(String.join("", highlights.get("searchTitle")));
                    }

                    // ▼▼▼ [수정] 하이라이트 처리 순서 변경 ▼▼▼
                    if (highlights.containsKey("searchContent")) {
                        // 1. 파일/스톤의 내용이 하이라이트된 경우
                        dto.setSearchContent(String.join(" ... ", highlights.get("searchContent")));
                    } else if (highlights.containsKey("docLines.content")) {
                        // 2. [추가] 문서의 라인 내용이 하이라이트된 경우
                        dto.setSearchContent(String.join(" ... ", highlights.get("docLines.content")));
                    } else if (dto.getSearchContent() != null) {
                        // 3. (파일/스톤용) 하이라이트 없고 원본 내용이 있을 경우 자르기
                        dto.setSearchContent(
                                dto.getSearchContent().length() > 200
                                        ? dto.getSearchContent().substring(0, 200) + "..."
                                        : dto.getSearchContent()
                        );
                    }
                    // ▲▲▲ [수정] 끝 ▲▲▲

                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * ✅ 통합 검색어 자동완성 (workspaceId 필터 추가됨)
     * (이 메소드는 제목(searchTitle)만 검색하므로 수정할 필요가 없습니다.)
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
                                                        .field("workspaceId") // Elasticsearch의 필드명
                                                        .value(workspaceId)
                                                )
                                        )
                                // ▲▲▲ [추가] 끝 ▲▲▲
                        )
                )
                .withPageable(pageable)
                .withSourceFilter(new FetchSourceFilter(
                        new String[]{"searchTitle", "docType", "fileUrl"}, null))
                .build();

        SearchHits<DocumentSuggestResDto> searchHits = elasticsearchOperations.search(
                query,
                DocumentSuggestResDto.class, // <-- 반환 DTO 클래스 지정
                ALL_INDICES
        );

        return searchHits.stream()
                .map(hit -> hit.getContent()) // <-- DTO 객체 추출
                .collect(Collectors.toList());
    }
}