package com.Dolmeng_E.search.domain.search.controller;

import com.Dolmeng_E.search.domain.search.service.SearchService;
import com.Dolmeng_E.search.domain.search.service.UnifiedSearchService;
import com.example.modulecommon.dto.CommonSuccessDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UnifiedSearchController {

    private final UnifiedSearchService unifiedSearchService;
    private final SearchService searchService;

    @GetMapping("/search")
    public ResponseEntity<?> searchDocument(@RequestHeader("X-User-Id") String userId, @RequestParam String keyword) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(unifiedSearchService.search(keyword, userId))
                .statusCode(HttpStatus.OK.value())
                .statusMessage("검색 성공")
                .build(), HttpStatus.OK);
    }

    @GetMapping("/suggest")
    public ResponseEntity<?> suggestDocument(@RequestHeader("X-User-Id") String userId, @RequestParam String keyword) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(unifiedSearchService.suggest(keyword, userId))
                .statusCode(HttpStatus.OK.value())
                .statusMessage("검색어 제안")
                .build(), HttpStatus.OK);
    }

    // 워크스페이스/프로젝트/스톤 삭제 시 문서함 삭제
    @DeleteMapping("/{rootType}/{rootId}/all")
    public void deleteAll(@PathVariable String rootType, @PathVariable String rootId) {
        searchService.deleteAll(rootType, rootId);
    }
}
