package com.Dolmeng_E.search.domain.search.controller;

import com.Dolmeng_E.search.domain.search.service.UnifiedSearchService;
import com.example.modulecommon.dto.CommonSuccessDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class UnifiedSearchController {

    private final UnifiedSearchService unifiedSearchService;

    @GetMapping
    public ResponseEntity<?> searchDocumentByTitle(@RequestParam String title) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(unifiedSearchService.searchDocumentByTitle(title))
                .statusCode(HttpStatus.OK.value())
                .statusMessage("이름을 통한 검색 성공")
                .build(), HttpStatus.OK);
    }
}
