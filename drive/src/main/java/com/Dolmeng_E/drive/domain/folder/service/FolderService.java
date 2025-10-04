package com.Dolmeng_E.drive.domain.folder.service;

import com.Dolmeng_E.drive.domain.folder.dto.FolderContentsDto;
import com.Dolmeng_E.drive.domain.folder.dto.FolderSaveDto;
import com.Dolmeng_E.drive.domain.folder.dto.FolderUpdateNameDto;
import com.Dolmeng_E.drive.domain.folder.entity.Folder;
import com.Dolmeng_E.drive.domain.folder.repository.FolderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class FolderService {

    private final FolderRepository folderRepository;

    // 폴더 생성
    public String createFolder(FolderSaveDto folderSaveDto){
        if(folderRepository.findByParentIdAndNameAndIsDeleteIsFalse(folderSaveDto.getParentId(), folderSaveDto.getName()).isPresent()){
            throw new IllegalArgumentException("중복된 폴더명입니다.");
        }
        return folderRepository.save(folderSaveDto.toEntity()).getId();
    }

    // 폴더명 수정
    public String updateFolderName(FolderUpdateNameDto folderUpdateNameDto, String id){
        Folder folder = folderRepository.findById(id).orElseThrow(()->new EntityNotFoundException("해당 폴더가 존재하지 않습니다."));
        if(folderRepository.findByParentIdAndNameAndIsDeleteIsFalse(folder.getParentId(), folderUpdateNameDto.getName()).isPresent()){
            throw new IllegalArgumentException("중복된 폴더명입니다.");
        }
        folder.updateName(folderUpdateNameDto.getName());
        return folder.getName();
    }

    // 폴더 삭제
    public String deleteFolder(String id){
        Folder folder = folderRepository.findById(id).orElseThrow(()->new EntityNotFoundException("해당 폴더가 존재하지 않습니다."));
        // 폴더 하위 폴더 및 하위 모두 isDelete 수정 -> 재귀 함수 호출
        performRecursiveSoftDelete(folder);
        return folder.getName();
    }
    
    // 폴더 삭제(소프트) 재귀 함수
    // 파일 삭제 로직 추가 필요
    private void performRecursiveSoftDelete(Folder folder){
        folder.updateIsDelete();
        List<Folder> childFolders = folderRepository.findAllByParentIdAndIsDeleteIsFalse(folder.getId());
        for(Folder childFolder : childFolders){
            performRecursiveSoftDelete(childFolder);
        }
    }
    
    // 폴더 하위 요소들 조회
    public List<FolderContentsDto> getFolderContents(String id){
        Folder folder = folderRepository.findById(id).orElseThrow(()->new EntityNotFoundException("해당 폴더가 존재하지 않습니다."));
        List<Folder> folders = folderRepository.findAllByParentIdAndIsDeleteIsFalse(id);
        // 파일 모두 불러오기

        List<FolderContentsDto> folderContentsDtos = new ArrayList<>();
        // 폴더 넣기
        for(Folder Childfolder : folders){
            folderContentsDtos.add(FolderContentsDto.builder()
                            .size("-")
                            .createBy(Childfolder.getCreatedBy())
                            .name(Childfolder.getName())
                            .updateAt(Childfolder.getUpdatedAt().toString())
                            .name(Childfolder.getName())
                            .type("폴더")
                    .build());
        }
        return folderContentsDtos;
    }
}
