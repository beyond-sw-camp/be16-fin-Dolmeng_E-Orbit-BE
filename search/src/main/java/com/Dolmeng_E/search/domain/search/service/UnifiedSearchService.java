package com.Dolmeng_E.search.domain.search.service;

import com.Dolmeng_E.search.domain.search.dto.DocumentResDto;
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
     * ✅ 통합 검색 수행
     */
    public List<DocumentResDto> search(String keyword, String currentUserId) {

        String[] searchFields = {"searchTitle", "searchContent"};
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
        HighlightQuery highlightQuery = new HighlightQuery(highlight, DocumentResDto.class);

        // ✅ 2. NativeQuery
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> b
                                .must(m -> m
                                        .multiMatch(mm -> mm
                                                .query(keyword)
                                                .fields(List.of(searchFields))
                                        )
                                )
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
        SearchHits<DocumentResDto> searchHits = elasticsearchOperations.search(
                query,
                DocumentResDto.class,
                ALL_INDICES
        );

        // ✅ 4. 결과 처리
        return searchHits.stream()
                .map(hit -> {
                    DocumentResDto dto = hit.getContent();
                    Map<String, List<String>> highlights = hit.getHighlightFields();

                    if (highlights.containsKey("searchTitle")) {
                        dto.setSearchTitle(String.join("", highlights.get("searchTitle")));
                    }
                    if (highlights.containsKey("searchContent")) {
                        dto.setSearchContent(String.join("", highlights.get("searchContent")));
                    }

                    // [수정] NPE(NullPointerException) 방어 코드 추가
                    if (dto.getSearchContent() != null && !highlights.containsKey("searchContent")) {
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
     * ✅ 통합 검색어 자동완성
     */
    public List<String> suggest(String keyword, String currentUserId) {

        String suggestField = "searchTitle.ngram";
        Pageable pageable = PageRequest.of(0, 10);

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> b
                                .must(m -> m
                                        .match(mq -> mq
                                                .field(suggestField)
                                                .query(keyword)
                                        )
                                )
                                .filter(f -> f
                                        .term(t -> t
                                                .field("viewableUserIds")
                                                .value(currentUserId)
                                        )
                                )
                        )
                )
                .withPageable(pageable)
                .withSourceFilter(new FetchSourceFilter(
                        new String[]{"searchTitle", "docType"}, null))
                .build();

        SearchHits<DocumentResDto> searchHits = elasticsearchOperations.search(
                query,
                DocumentResDto.class,
                ALL_INDICES
        );

        return searchHits.stream()
                .map(hit -> hit.getContent().getSearchTitle())
                .distinct()
                .collect(Collectors.toList());
    }
}

