package com.Dolmeng_E.drive.domain.drive.controller;

import com.Dolmeng_E.drive.domain.drive.dto.DocumentLineCreateReqDto;
import com.Dolmeng_E.drive.domain.drive.dto.FolderSaveDto;
import com.Dolmeng_E.drive.domain.drive.service.DocumentLineService;
import com.example.modulecommon.dto.CommonSuccessDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/documentLine")
@RequiredArgsConstructor
public class DocumentLineController {
    private final DocumentLineService documentLineService;

    // 새 문서 라인 생성
    @PostMapping("")
    public ResponseEntity<?> saveDocumentLine(@RequestBody DocumentLineCreateReqDto documentLineCreateReqDto) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(documentLineService.createDocumentLine(documentLineCreateReqDto))
                .statusCode(HttpStatus.CREATED.value())
                .statusMessage("문서 라인 생성 성공")
                .build(), HttpStatus.CREATED);
    }

    // 특정 문서의 모든 라인 조회
    @GetMapping("/document/{documentId}/documentLines")
    public ResponseEntity<?> getDocumentLinesByDocumentId(@PathVariable String documentId) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(documentLineService.findAllDocumentLinesByDocumentId(documentId))
                .statusCode(HttpStatus.OK.value())
                .statusMessage("문서의 모든 라인 조회 성공")
                .build(), HttpStatus.OK);
    }
}
