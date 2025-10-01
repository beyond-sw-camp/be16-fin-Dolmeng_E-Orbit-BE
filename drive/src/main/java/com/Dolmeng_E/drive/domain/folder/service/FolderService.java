package com.Dolmeng_E.drive.domain.folder.service;

import com.Dolmeng_E.drive.domain.folder.dto.CreateFolderDto;
import com.Dolmeng_E.drive.domain.folder.entity.Folder;
import com.Dolmeng_E.drive.domain.folder.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static jakarta.persistence.GenerationType.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class FolderService {

    private final FolderRepository folderRepository;

    public String createFolder(CreateFolderDto createFolderDto){
        if(folderRepository.findByParentIdAndName(createFolderDto.getParentId(), createFolderDto.getName()).isPresent()){
            throw new IllegalArgumentException("중복된 폴더명입니다.");
        }
        return folderRepository.save(createFolderDto.toEntity()).getId();
    }
}
