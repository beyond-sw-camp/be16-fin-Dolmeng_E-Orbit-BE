package com.Dolmeng_E.drive.domain.drive.controller;

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

    // 특정 문서의 모든 라인 조회
    @GetMapping("/document/{documentId}/documentLines")
    public ResponseEntity<?> getDocumentLinesByDocumentId(@PathVariable String documentId) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(documentLineService.findAllDocumentLinesByDocumentId(documentId))
                .statusCode(HttpStatus.OK.value())
                .statusMessage("문서의 모든 라인 조회 성공")
                .build(), HttpStatus.OK);
    }
    
    // 온라인 사용자 목록 조회
    @GetMapping("/document/{documentId}/online-users")
    public ResponseEntity<?> getOnlineUsersByDocumentId(@PathVariable String documentId) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(documentLineService.findAllOnlineUsersByDocumentId(documentId))
                .statusCode(HttpStatus.OK.value())
                .statusMessage("문서의 모든 온라인 사용자 목록 조회 성공")
                .build(), HttpStatus.OK);
    }
    
    @GetMapping("/userInfo")
    public ResponseEntity<?> getUserInfo(@RequestHeader("X-User-Id") String userId) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(documentLineService.getUserInfo(userId))
                .statusCode(HttpStatus.OK.value())
                .statusMessage("정보 조회 성공")
                .build(), HttpStatus.OK);
    }
}
