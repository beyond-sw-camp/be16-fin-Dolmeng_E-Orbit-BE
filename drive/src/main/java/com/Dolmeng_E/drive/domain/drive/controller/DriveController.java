package com.Dolmeng_E.drive.domain.drive.controller;

import com.Dolmeng_E.drive.domain.drive.dto.*;
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

    // 폴더 정보 조회
    @GetMapping("/folder/{folder_id}")
    public ResponseEntity<?> saveFolder(@PathVariable("folder_id") String folderId) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(driverService.getFolderInfo(folderId))
                .statusCode(HttpStatus.OK.value())
                .statusMessage("폴더 정보 조회 성공")
                .build(), HttpStatus.OK);
    }

    // 폴더 생성
    @PostMapping("/folder")
    public ResponseEntity<?> saveFolder(@RequestHeader("X-User-Id") String userId, @RequestBody FolderSaveDto folderSaveDto) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(driverService.createFolder(folderSaveDto, userId))
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

    // 위치 별 하위 항목들 가져오기
    @GetMapping("/{rootType}/{rootId}")
    public ResponseEntity<?> getContents(@RequestHeader("X-User-Id") String userId, @RequestHeader("X-Workspace-Id") String workspaceId, @PathVariable String rootId, @PathVariable String rootType) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(driverService.getContents(rootId, userId, rootType, workspaceId))
                .statusCode(HttpStatus.OK.value())
                .statusMessage("루트 하위 요소들 조회 성공")
                .build(), HttpStatus.OK);
    }

    // 폴더 하위 요소들 조회
    @GetMapping("/folder/{folderId}/contents")
    public ResponseEntity<?> getFolderContents(@RequestHeader("X-User-Id") String userId, @PathVariable String folderId) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(driverService.getFolderContents(folderId, userId))
                .statusCode(HttpStatus.OK.value())
                .statusMessage("폴더 하위 요소들 조회 성공")
                .build(), HttpStatus.OK);
    }

    // 폴더 옮기기
    @PutMapping("/folder/{folderId}/move")
    public ResponseEntity<?> updateFolderStruct(@RequestHeader("X-User-Id") String userId, @PathVariable String folderId, @RequestBody FolderMoveDto folderMoveDto) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(driverService.updateFolderStruct(folderId, folderMoveDto))
                .statusCode(HttpStatus.OK.value())
                .statusMessage("폴더 위치 변경 성공")
                .build(), HttpStatus.OK);
    }

    // 파일 업로드
    @PostMapping("/folder/{folderId}/file")
    public ResponseEntity<?> uploadFile(@RequestHeader("X-User-Id") String userId, @PathVariable String folderId, @ModelAttribute FileSaveDto fileSaveDto) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(driverService.uploadFile(userId, folderId, fileSaveDto))
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

    // 파일/문서 옮기기
    @PutMapping("/element/{elementId}/move")
    public ResponseEntity<?> updateElementStruct(@RequestHeader("X-User-Id") String userId, @PathVariable String elementId, @RequestBody ElementMoveDto elementMoveDto) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(driverService.updateElementStruct(elementId, elementMoveDto))
                .statusCode(HttpStatus.OK.value())
                .statusMessage("문서/파일 위치 변경 성공")
                .build(), HttpStatus.OK);
    }

    // 문서 생성
    @PostMapping("/folder/{folderId}/document")
    public ResponseEntity<?> saveDocument(@RequestHeader("X-User-Id") String userId, @PathVariable String folderId, @RequestBody DocumentSaveDto documentSaveDto) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(driverService.createDocument(userId, folderId, documentSaveDto))
                .statusCode(HttpStatus.CREATED.value())
                .statusMessage("문서 생성 성공")
                .build(), HttpStatus.CREATED);
    }

    // 문서 삭제
    @DeleteMapping("/document/{documentId}")
    public ResponseEntity<?> deleteDocument(@RequestHeader("X-User-Id") String userId, @PathVariable String documentId) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(driverService.deleteDocument(documentId))
                .statusCode(HttpStatus.OK.value())
                .statusMessage("문서 삭제 성공")
                .build(), HttpStatus.OK);
    }

    // 문서 조회
    @GetMapping("/document/{documentId}")
    public ResponseEntity<?> getDocument(@RequestHeader("X-User-Id") String userId, @PathVariable String documentId) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(driverService.findDocument(userId, documentId))
                .statusCode(HttpStatus.OK.value())
                .statusMessage("문서 조회 성공")
                .build(), HttpStatus.OK);
    }

    // 문서 수정
    @PutMapping("/document/{documentId}")
    public ResponseEntity<?> updateDocument(@PathVariable String documentId, @RequestBody DocumentUpdateDto documentUpdateDto) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(driverService.updateDocument(documentId, documentUpdateDto))
                .statusCode(HttpStatus.OK.value())
                .statusMessage("문서 조회 성공")
                .build(), HttpStatus.OK);
    }
    
    // 워크스페이스 사용량 조회
    @GetMapping("/files/storage")
    public ResponseEntity<?> getFilesSize(@RequestHeader("X-Workspace-Id") String workspaceId) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(driverService.getFilesSize(workspaceId))
                .statusCode(HttpStatus.OK.value())
                .statusMessage("스토리지 사용량 조회 성공")
                .build(), HttpStatus.OK);
    }
}
