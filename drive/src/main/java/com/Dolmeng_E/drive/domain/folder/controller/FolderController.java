package com.Dolmeng_E.drive.domain.folder.controller;

import com.Dolmeng_E.drive.common.dto.CommonSuccessDto;
import com.Dolmeng_E.drive.domain.folder.dto.FolderSaveDto;
import com.Dolmeng_E.drive.domain.folder.dto.FolderUpdateNameDto;
import com.Dolmeng_E.drive.domain.folder.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/folder")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    // 폴더 생성
    @PostMapping("")
    public ResponseEntity<?> saveFolder(@RequestBody FolderSaveDto folderSaveDto) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(folderService.createFolder(folderSaveDto))
                .statusCode(HttpStatus.CREATED.value())
                .statusMessage("폴더 생성 성공")
                .build(), HttpStatus.CREATED);
    }

    // 폴더명 수정
    @PutMapping("/{folder_id}")
    public ResponseEntity<?> updateFolder(@RequestBody FolderUpdateNameDto folderUpdateNameDto, @PathVariable String folder_id) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(folderService.updateFolderName(folderUpdateNameDto, folder_id))
                .statusCode(HttpStatus.OK.value())
                .statusMessage("폴더명 수정 성공")
                .build(), HttpStatus.OK);
    }

    // 폴더 삭제
    @DeleteMapping("/{folder_id}")
    public ResponseEntity<?> deleteFolder(@PathVariable String folder_id) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(folderService.deleteFolder(folder_id))
                .statusCode(HttpStatus.OK.value())
                .statusMessage("폴더 삭제 성공")
                .build(), HttpStatus.OK);
    }

    // 폴더 하위 요소들 조회
    @GetMapping("/{folder_id}/contents")
    public ResponseEntity<?> getFolderContents(@PathVariable String folder_id) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(folderService.getFolderContents(folder_id))
                .statusCode(HttpStatus.OK.value())
                .statusMessage("폴더 하위 요소들 조회 성공")
                .build(), HttpStatus.OK);
    }
}
