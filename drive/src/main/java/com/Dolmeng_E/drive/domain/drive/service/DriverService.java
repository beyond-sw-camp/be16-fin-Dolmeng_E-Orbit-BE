package com.Dolmeng_E.drive.domain.drive.service;

import com.Dolmeng_E.drive.domain.drive.dto.FolderContentsDto;
import com.Dolmeng_E.drive.domain.drive.dto.FolderSaveDto;
import com.Dolmeng_E.drive.domain.drive.dto.FolderUpdateNameDto;
import com.Dolmeng_E.drive.domain.drive.entity.Document;
import com.Dolmeng_E.drive.domain.drive.entity.File;
import com.Dolmeng_E.drive.domain.drive.entity.Folder;
import com.Dolmeng_E.drive.domain.drive.repository.DocumentRepository;
import com.Dolmeng_E.drive.domain.drive.repository.FileRepository;
import com.Dolmeng_E.drive.domain.drive.repository.FolderRepository;
import com.example.modulecommon.service.S3Uploader;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class DriverService {

    private final FolderRepository folderRepository;
    private final S3Uploader s3Uploader;
    private final FileRepository fileRepository;
    private final DocumentRepository documentRepository;

    // 폴더 생성
    public String createFolder(FolderSaveDto folderSaveDto){
        if(folderRepository.findByParentIdAndNameAndIsDeleteIsFalse(folderSaveDto.getParentId(), folderSaveDto.getName()).isPresent()){
            throw new IllegalArgumentException("중복된 폴더명입니다.");
        }
        return folderRepository.save(folderSaveDto.toEntity()).getId();
    }

    // 폴더명 수정
    public String updateFolderName(FolderUpdateNameDto folderUpdateNameDto, String folderId){
        Folder folder = folderRepository.findById(folderId).orElseThrow(()->new EntityNotFoundException("해당 폴더가 존재하지 않습니다."));
        if(folderRepository.findByParentIdAndNameAndIsDeleteIsFalse(folder.getParentId(), folderUpdateNameDto.getName()).isPresent()){
            throw new IllegalArgumentException("중복된 폴더명입니다.");
        }
        folder.updateName(folderUpdateNameDto.getName());
        return folder.getName();
    }

    // 폴더 삭제
    public String deleteFolder(String folderId){
        Folder folder = folderRepository.findById(folderId).orElseThrow(()->new EntityNotFoundException("해당 폴더가 존재하지 않습니다."));
        // 폴더 하위 폴더 및 하위 모두 isDelete 수정 -> 재귀 함수 호출
        performRecursiveSoftDelete(folder);
        return folder.getName();
    }
    
    // 폴더 삭제(소프트) 재귀 함수
    private void performRecursiveSoftDelete(Folder folder){
        folder.updateIsDelete();
        List<Folder> childFolders = folderRepository.findAllByParentIdAndIsDeleteIsFalse(folder.getId());
        for(Folder childFolder : childFolders){
            performRecursiveSoftDelete(childFolder);
        }
        for(File file : folder.getFiles()){
            file.updateIsDelete();
        }
        for(Document document : folder.getDocuments()){
            document.updateIsDelete();
        }
    }
    
    // 폴더 하위 요소들 조회
    public List<FolderContentsDto> getFolderContents(String folderId){
        Folder folder = folderRepository.findById(folderId).orElseThrow(()->new EntityNotFoundException("해당 폴더가 존재하지 않습니다."));
        List<Folder> folders = folderRepository.findAllByParentIdAndIsDeleteIsFalse(folderId);
        List<FolderContentsDto> folderContentsDtos = new ArrayList<>();
        // 하위 폴더 불러오기
        for(Folder childfolder : folders){
            if(childfolder.getIsDelete().equals(true)) continue;
            folderContentsDtos.add(FolderContentsDto.builder()
                            .createBy(childfolder.getCreatedBy())
                            .name(childfolder.getName())
                            .updateAt(childfolder.getUpdatedAt().toString())
                            .type("folder")
                    .build());
        }
        // 파일 불러오기
        for(File file : folder.getFiles()){
            if(file.getIsDelete().equals(true)) continue;
            folderContentsDtos.add(FolderContentsDto.builder()
                    .size(file.getSize())
                    .createBy(file.getCreatedBy())
                    .name(file.getName())
                    .type(file.getType())
                    .build());
        }
        // 문서 불러오기
        for(Document document : folder.getDocuments()){
            if(document.getIsDelete().equals(true)) continue;
            folderContentsDtos.add(FolderContentsDto.builder()
                    .createBy(document.getCreatedBy())
                    .updateAt(document.getUpdatedBy())
                    .name(document.getTitle())
                    .type("document")
                    .build());
        }
        return folderContentsDtos;
    }

    // 파일 업로드
    public String uploadFile(MultipartFile file, String folderId){
        String file_url = s3Uploader.upload(file, "drive");
        Folder folder = folderRepository.findById(folderId).orElseThrow(()->new EntityNotFoundException("해당 폴더가 존재하지 않습니다."));
        if(fileRepository.findByFolderAndNameAndIsDeleteFalse(folder, file.getOriginalFilename()).isPresent()){
            throw new IllegalArgumentException("동일한 이름의 파일이 존재합니다.");
        }
        File fileEntity = File.builder()
                .url(file_url)
                .createdBy("회원ID")
                .folder(folder)
                .name(file.getOriginalFilename())
                .type(file.getContentType())
                .size(file.getSize())
                .build();
        return fileRepository.save(fileEntity).getId();
    }

    // 파일 삭제(소프트)
    public String deleteFile(String fileId){
        File file = fileRepository.findById(fileId).orElseThrow(()->new EntityNotFoundException("파일이 존재하지 않습니다."));
        String fileName = file.getName();
        s3Uploader.delete(file.getUrl());
        file.updateIsDelete();
        return fileName;
    }

    // 문서 생성
    public String createDocument(String folderId, String documentTitle){
        Folder folder = folderRepository.findById(folderId).orElseThrow(()->new EntityNotFoundException("해당 폴더가 존재하지 않습니다."));
        if(documentRepository.findByFolderAndTitleAndIsDeleteFalse(folder, documentTitle).isPresent()){
            throw new IllegalArgumentException("동일한 이름의 문서가 존재합니다.");
        }
        Document document = Document.builder()
                .createdBy("회원ID")
                .title(documentTitle)
                .folder(folder)
                .build();
        return documentRepository.save(document).getId();
    }

    // 문서 삭제(소프트)
    public String deleteDocument(String documentId){
        Document document = documentRepository.findById(documentId).orElseThrow(()->new EntityNotFoundException("해당 문서가 존재하지 않습니다."));
        document.updateIsDelete();
        return document.getTitle();
    }
}
