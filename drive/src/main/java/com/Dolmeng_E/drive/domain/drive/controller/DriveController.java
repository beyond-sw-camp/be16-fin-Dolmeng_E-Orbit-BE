package com.Dolmeng_E.drive.domain.drive.controller;

import com.Dolmeng_E.drive.domain.drive.dto.FolderSaveDto;
import com.Dolmeng_E.drive.domain.drive.dto.FolderUpdateNameDto;
import com.Dolmeng_E.drive.domain.drive.service.DriverService;
import com.example.modulecommon.dto.CommonSuccessDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/drive")
@RequiredArgsConstructor
public class DriveController {

    private final DriverService driverService;

    // 폴더 생성
    @PostMapping("/folder")
    public ResponseEntity<?> saveFolder(@RequestBody FolderSaveDto folderSaveDto) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(driverService.createFolder(folderSaveDto))
                .statusCode(HttpStatus.CREATED.value())
                .statusMessage("폴더 생성 성공")
                .build(), HttpStatus.CREATED);
    }

    // 폴더명 수정
    @PutMapping("/folder/{folderId}")
    public ResponseEntity<?> updateFolder(@RequestBody FolderUpdateNameDto folderUpdateNameDto, @PathVariable String folderId) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(driverService.updateFolderName(folderUpdateNameDto, folderId))
                .statusCode(HttpStatus.OK.value())
                .statusMessage("폴더명 수정 성공")
                .build(), HttpStatus.OK);
    }

    // 폴더 삭제
    @DeleteMapping("/folder/{folderId}")
    public ResponseEntity<?> deleteFolder(@PathVariable String folderId) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(driverService.deleteFolder(folderId))
                .statusCode(HttpStatus.OK.value())
                .statusMessage("폴더 삭제 성공")
                .build(), HttpStatus.OK);
    }

    // 폴더 하위 요소들 조회
    @GetMapping("/folder/{folderId}/contents")
    public ResponseEntity<?> getFolderContents(@PathVariable String folderId) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(driverService.getFolderContents(folderId))
                .statusCode(HttpStatus.OK.value())
                .statusMessage("폴더 하위 요소들 조회 성공")
                .build(), HttpStatus.OK);
    }

    // 파일 업로드
    @PostMapping("/folder/{folderId}/file")
    public ResponseEntity<?> uploadFile(@PathVariable String folderId, @RequestParam MultipartFile file) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(driverService.uploadFile(file, folderId))
                .statusCode(HttpStatus.CREATED.value())
                .statusMessage("파일 업로드 성공")
                .build(), HttpStatus.CREATED);
    }

    // 파일 삭제
    @DeleteMapping("/file/{fileId}")
    public ResponseEntity<?> deleteFile(@PathVariable String fileId) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(driverService.deleteFile(fileId))
                .statusCode(HttpStatus.OK.value())
                .statusMessage("파일 삭제 성공")
                .build(), HttpStatus.OK);
    }

    // 문서 생성
    @PostMapping("/folder/{folderId}/document")
    public ResponseEntity<?> saveDocument(@PathVariable String folderId, @RequestParam String documentTitle) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(driverService.createDocument(folderId, documentTitle))
                .statusCode(HttpStatus.CREATED.value())
                .statusMessage("문서 생성 성공")
                .build(), HttpStatus.CREATED);
    }

    // 문서 삭제
    @DeleteMapping("/document/{documentId}")
    public ResponseEntity<?> deleteDocument(@PathVariable String documentId) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(driverService.deleteDocument(documentId))
                .statusCode(HttpStatus.OK.value())
                .statusMessage("문서 삭제 성공")
                .build(), HttpStatus.OK);
    }
}
