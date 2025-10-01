package com.Dolmeng_E.drive.domain.folder.controller;

import com.Dolmeng_E.drive.common.dto.CommonSuccessDto;
import com.Dolmeng_E.drive.domain.folder.dto.CreateFolderDto;
import com.Dolmeng_E.drive.domain.folder.entity.Folder;
import com.Dolmeng_E.drive.domain.folder.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/folder")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    // 폴더 생성
    @PostMapping("")
    public ResponseEntity<?> createFolder(@RequestBody CreateFolderDto createFolderDto) {
        return new ResponseEntity<>(CommonSuccessDto.builder()
                .result(folderService.createFolder(createFolderDto))
                .statusCode(HttpStatus.CREATED.value())
                .statusMessage("폴더 생성 성공")
                .build(), HttpStatus.CREATED);
    }
}
