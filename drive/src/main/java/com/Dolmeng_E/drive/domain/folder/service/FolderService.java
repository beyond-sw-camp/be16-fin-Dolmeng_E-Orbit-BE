package com.Dolmeng_E.drive.domain.folder.service;

import com.Dolmeng_E.drive.domain.folder.dto.CreateFolderDto;
import com.Dolmeng_E.drive.domain.folder.dto.UpdateFolderNameDto;
import com.Dolmeng_E.drive.domain.folder.entity.Folder;
import com.Dolmeng_E.drive.domain.folder.repository.FolderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class FolderService {

    private final FolderRepository folderRepository;

    // 폴더 생성
    public String createFolder(CreateFolderDto createFolderDto){
        if(folderRepository.findByParentIdAndName(createFolderDto.getParentId(), createFolderDto.getName()).isPresent()){
            throw new IllegalArgumentException("중복된 폴더명입니다.");
        }
        return folderRepository.save(createFolderDto.toEntity()).getId();
    }

    // 폴더명 수정
    public String updateFolderName(UpdateFolderNameDto updateFolderNameDto, String id){
        Folder folder = folderRepository.findById(id).orElseThrow(()->new EntityNotFoundException("해당 폴더가 존재하지 않습니다."));
        if(folderRepository.findByParentIdAndName(folder.getParentId(), updateFolderNameDto.getName()).isPresent()){
            throw new IllegalArgumentException("중복된 폴더명입니다.");
        }
        folder.updateName(updateFolderNameDto.getName());
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
        List<Folder> childFolders = folderRepository.findByParentIdAndIsDeleteIsFalse(folder.getId());
        for(Folder childFolder : childFolders){
            performRecursiveSoftDelete(childFolder);
        }
    }
}
