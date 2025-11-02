package com.Dolmeng_E.drive.domain.drive.service;

import com.Dolmeng_E.drive.common.controller.WorkspaceServiceClient;
import com.Dolmeng_E.drive.common.dto.StoneTaskResDto;
import com.Dolmeng_E.drive.common.dto.SubProjectResDto;
import com.Dolmeng_E.drive.common.dto.WorkspaceOrProjectManagerCheckDto;
import com.Dolmeng_E.drive.common.service.S3Uploader;
import com.Dolmeng_E.drive.domain.drive.dto.*;
import com.Dolmeng_E.drive.domain.drive.entity.Document;
import com.Dolmeng_E.drive.domain.drive.entity.File;
import com.Dolmeng_E.drive.domain.drive.entity.Folder;
import com.Dolmeng_E.drive.domain.drive.entity.RootType;
import com.Dolmeng_E.drive.domain.drive.repository.DocumentRepository;
import com.Dolmeng_E.drive.domain.drive.repository.FileRepository;
import com.Dolmeng_E.drive.domain.drive.repository.FolderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@Transactional
public class DriverService {

    private final FolderRepository folderRepository;
    private final S3Uploader s3Uploader;
    private final FileRepository fileRepository;
    private final DocumentRepository documentRepository;
    private final KafkaTemplate<String, String> kafkaTemplate; // Kafka 전송용
    private final ObjectMapper objectMapper;
    private final HashOperations<String, String, String> hashOperations;
    private final WorkspaceServiceClient workspaceServiceClient;

    public DriverService(FolderRepository folderRepository, S3Uploader s3Uploader, FileRepository fileRepository, DocumentRepository documentRepository, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, @Qualifier("userInventory") RedisTemplate<String, String> redisTemplate, WorkspaceServiceClient workspaceServiceClient) {
        this.folderRepository = folderRepository;
        this.s3Uploader = s3Uploader;
        this.fileRepository = fileRepository;
        this.documentRepository = documentRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.hashOperations = redisTemplate.opsForHash();
        this.workspaceServiceClient = workspaceServiceClient;
    }

    // 폴더 생성
    public String createFolder(FolderSaveDto folderSaveDto, String userId){
        if(folderRepository.findByParentIdAndNameAndIsDeleteIsFalse(folderSaveDto.getParentId(), folderSaveDto.getName()).isPresent()){
            throw new IllegalArgumentException("이미 존재하는 폴더명입니다.");
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

    // 폴더 정보 조회
    public FolderInfoResDto getFolderInfo(String folderId){
        Folder folder = folderRepository.findById(folderId).orElseThrow(()->new EntityNotFoundException("해당 폴더가 존재하지 않습니다."));
        return FolderInfoResDto.builder()
                .folderId(folderId)
                .folderName(folder.getName())
                .ancestors(folderRepository.findAncestors(folderId))
                .build();
    }
    
    // 폴더 하위 요소들 조회
    public List<FolderContentsDto> getFolderContents(String folderId, String userId){
        Map<String, String> userInfo = hashOperations.entries("user:"+userId);
        Folder folder = folderRepository.findById(folderId).orElseThrow(()->new EntityNotFoundException("해당 폴더가 존재하지 않습니다."));
        List<Folder> folders = folderRepository.findAllByParentIdAndIsDeleteIsFalse(folderId);
        List<FolderContentsDto> folderContentsDtos = new ArrayList<>();
        // 하위 폴더 불러오기
        for(Folder childfolder : folders){
            List<FolderInfoDto> ancestors = folderRepository.findAncestors(childfolder.getId());
            folderContentsDtos.add(FolderContentsDto.builder()
                            .createBy(childfolder.getCreatedBy())
                            .name(childfolder.getName())
                            .updateAt(childfolder.getUpdatedAt().toString())
                            .id(childfolder.getId())
                            .type("folder")
                            .creatorName(userInfo.get("name"))
                            .profileImage(userInfo.get("profileImageUrl"))
                            .ancestors(ancestors)
                    .build());
        }
        // 파일 불러오기
        for(File file : folder.getFiles()){
            if(file.getIsDelete().equals(true)) continue;
            folderContentsDtos.add(FolderContentsDto.builder()
                    .size(file.getSize())
                    .createBy(file.getCreatedBy())
                    .updateAt(file.getUpdatedAt().toString())
                    .name(file.getName())
                    .type(file.getType())
                    .id(file.getId())
                    .creatorName(userInfo.get("name"))
                    .profileImage(userInfo.get("profileImageUrl"))
                    .url(file.getUrl())
                    .build());
        }
        // 문서 불러오기
        for(Document document : folder.getDocuments()){
            if(document.getIsDelete().equals(true)) continue;
            folderContentsDtos.add(FolderContentsDto.builder()
                    .createBy(document.getCreatedBy())
                    .updateAt(document.getUpdatedAt().toString())
                    .name(document.getTitle())
                    .type("document")
                    .id(document.getId())
                    .creatorName(userInfo.get("name"))
                    .profileImage(userInfo.get("profileImageUrl"))
                    .build());
        }
        return folderContentsDtos;
    }

    // 루트 하위 요소들 조회 + 바로가기(ex. 워크스페이스의 하위 스톤 드라이브로 바로가기)
    public List<RootContentsDto> getContents(String rootId, String userId, String rootType, String workspaceId){
        List<RootContentsDto> rootContentsDtos = new ArrayList<>();
        if(rootType.equals("WORKSPACE")){
            try {
                List<SubProjectResDto> projects = workspaceServiceClient.getSubProjectsByWorkspace(rootId);
                for(SubProjectResDto project : projects){
                    rootContentsDtos.add(RootContentsDto.builder()
                            .id(project.getProjectId())
                            .name(project.getProjectName())
                            .type("PROJECT")
                            .build());
                }
            }catch (FeignException e){
                System.out.println(e.getMessage());
                throw new IllegalArgumentException("예상치못한오류 발생");
            }
        }else if(rootType.equals("PROJECT")){
            try {
                if (Objects.requireNonNull(workspaceServiceClient.checkProjectMembership(rootId, userId).getBody()).getResult().equals(false)
                        &&Objects.requireNonNull(workspaceServiceClient.checkWorkspaceManager(workspaceId, userId).getBody()).getResult().equals(false)){
                    throw new IllegalArgumentException("권한이 없습니다.");
                }
            }catch (FeignException e){
                System.out.println(e.getMessage());
                throw new IllegalArgumentException("예상치못한오류 발생");
            }
            try {
                StoneTaskResDto stoneTaskResDto = workspaceServiceClient.getSubStonesAndTasks(rootId);
                List<StoneTaskResDto.StoneInfo> stones = stoneTaskResDto.getStones();
                for(StoneTaskResDto.StoneInfo stone : stones){
                    rootContentsDtos.add(RootContentsDto.builder()
                            .id(stone.getStoneId())
                            .name(stone.getStoneName())
                            .type("STONE")
                            .build());
                }
            }catch (Exception e){
                throw new IllegalArgumentException("예상치못한오류 발생");
            }
        }else if(rootType.equals("STONE")){
            try {
                WorkspaceOrProjectManagerCheckDto workspaceOrProjectManagerCheckDto = workspaceServiceClient.checkWorkspaceOrProjectManager(rootId, userId);
                if(Objects.requireNonNull(workspaceServiceClient.checkStoneMembership(rootId, userId).getBody()).getResult().equals(false)
                        && !workspaceOrProjectManagerCheckDto.isProjectManager() && !workspaceOrProjectManagerCheckDto.isWorkspaceManager()
                        && Objects.requireNonNull(workspaceServiceClient.checkStoneManagership(rootId, userId).getBody()).getResult().equals(false)
                        ){
                    throw new IllegalArgumentException("권한이 없습니다.");
                }
            }catch (FeignException e){
                System.out.println(e.getMessage());
                throw new IllegalArgumentException("예상치못한오류 발생");
            }
        }
        List<Folder> folders = folderRepository.findAllByParentIdIsNullAndRootTypeAndRootIdAndIsDeleteIsFalse(RootType.valueOf(rootType),rootId);
        for(Folder folder : folders){
            String getUserId = folder.getCreatedBy();
            Map<String, String> userInfo = hashOperations.entries("user:"+getUserId);
            List<FolderInfoDto> ancestors = folderRepository.findAncestors(folder.getId());
            rootContentsDtos.add(RootContentsDto.builder()
                    .createBy(folder.getCreatedBy())
                    .name(folder.getName())
                    .updateAt(folder.getUpdatedAt().toString())
                    .id(folder.getId())
                    .type("folder")
                    .creatorName(userInfo.get("name"))
                    .profileImage(userInfo.get("profileImageUrl"))
                    .ancestors(ancestors)
                    .build());
        }
        // 파일 불러오기
        List<File> files = fileRepository.findAllByFolderIsNullAndRootTypeAndRootIdAndIsDeleteIsFalse(RootType.valueOf(rootType),rootId);
        for(File file : files){
            String getUserId = file.getCreatedBy();
            Map<String, String> userInfo = hashOperations.entries("user:"+getUserId);
            rootContentsDtos.add(RootContentsDto.builder()
                    .size(file.getSize())
                    .createBy(file.getCreatedBy())
                    .updateAt(file.getUpdatedAt().toString())
                    .name(file.getName())
                    .type(file.getType())
                    .id(file.getId())
                    .creatorName(userInfo.get("name"))
                    .profileImage(userInfo.get("profileImageUrl"))
                    .url(file.getUrl())
                    .build());
        }
        // 문서 불러오기
        List<Document> documents = documentRepository.findAllByFolderIsNullAndRootTypeAndRootIdAndIsDeleteIsFalse(RootType.valueOf(rootType),rootId);
        for(Document document : documents){
            String getUserId = document.getCreatedBy();
            Map<String, String> userInfo = hashOperations.entries("user:"+getUserId);
            rootContentsDtos.add(RootContentsDto.builder()
                    .createBy(document.getCreatedBy())
                    .updateAt(document.getUpdatedAt().toString())
                    .name(document.getTitle())
                    .type("document")
                    .id(document.getId())
                    .creatorName(userInfo.get("name"))
                    .profileImage(userInfo.get("profileImageUrl"))
                    .build());
        }
        return rootContentsDtos;
    }

    // 폴더 옮기기
    public String updateFolderStruct(String folderId, FolderMoveDto folderMoveDto){
        Folder folder = folderRepository.findById(folderId).orElseThrow(()->new EntityNotFoundException("해당 폴더는 존재하지 않습니다"));
        if(folderRepository.findByParentIdAndNameAndIsDeleteIsFalse(folderMoveDto.getParentId(), folder.getName()).isPresent()){
            throw new IllegalArgumentException("중복된 폴더명입니다.");
        }
        folder.updateParentId(folderMoveDto.getParentId());
        return folder.getParentId();
    }

    // 파일/문서 옮기기
    public String updateElementStruct(String elementId, ElementMoveDto elementMoveDto){
        Optional<Folder> folder = Optional.empty();
        if(elementMoveDto.getFolderId()!=null){
            folder = folderRepository.findById(elementMoveDto.getFolderId());
        }
        if(elementMoveDto.getType().equals("document")){
            Document document = documentRepository.findById(elementId).orElseThrow(()->new EntityNotFoundException("해당 문서가 존재하지 않습니다."));
            document.updateFolder(folder.orElse(null));
            return document.getTitle();
        }
        if(elementMoveDto.getType().equals("file")){
            File file = fileRepository.findById(elementId).orElseThrow(()->new EntityNotFoundException("해당 파일이 존재하지 않습니다."));
            file.updateFolder(folder.orElse(null));
            return file.getName();
        }
        else throw new IllegalArgumentException("예기치 못한 오류 발생");
    }

    // 파일 업로드
    public String uploadFile(String userId, String folderId, FileSaveDto fileSaveDto){
        for(MultipartFile file : fileSaveDto.getFile()){
            String file_url = s3Uploader.upload(file, "drive");
            Optional<Folder> folder = folderRepository.findById(folderId);
            // 폴더가 있을 경우
            if(folder.isPresent()){
                if(fileRepository.findByFolderAndNameAndIsDeleteFalse(folder.get(), file.getOriginalFilename()).isPresent()){
                    throw new IllegalArgumentException("동일한 이름의 파일이 존재합니다.");
                }
            }
            // 상위 파일일 경우
            else{
                if(fileRepository.findByRootIdAndNameAndIsDeleteFalse(fileSaveDto.getRootId(), file.getOriginalFilename()).isPresent()){
                    throw new IllegalArgumentException("동일한 이름의 파일이 존재합니다.");
                }
            }
            File fileEntity = File.builder()
                    .url(file_url)
                    .createdBy(userId)
                    .folder(folder.orElse(null))
                    .name(file.getOriginalFilename())
                    .type("file")
                    .size(file.getSize())
                    .workspaceId(fileSaveDto.getWorkspaceId())
                    .rootId(fileSaveDto.getRootId())
                    .rootType(RootType.valueOf(fileSaveDto.getRootType()))
                    .build();
            fileRepository.save(fileEntity).getId();
        }

        return "파일 업로드 성공";
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
    public String createDocument(String userId, String folderId, DocumentSaveDto documentSaveDto){
        Optional<Folder> folder = folderRepository.findById(folderId);
        // 폴더가 있을 경우
        if(folder.isPresent()){
            if(documentRepository.findByFolderAndTitleAndIsDeleteFalse(folder.get(), documentSaveDto.getName()).isPresent()){
                throw new IllegalArgumentException("동일한 이름의 문서가 존재합니다.");
            }
        }
        // 상위 파일일 경우
        else{
            if(documentRepository.findByRootIdAndTitleAndIsDeleteFalse(documentSaveDto.getRootId(), documentSaveDto.getName()).isPresent()){
                throw new IllegalArgumentException("동일한 이름의 문서가 존재합니다.");
            }
        }
        Document document = Document.builder()
                .title(documentSaveDto.getName())
                .createdBy(userId)
                .folder(folder.orElse(null))
                .rootId(documentSaveDto.getRootId())
                .rootType(RootType.valueOf(documentSaveDto.getRootType()))
                .build();
        Document savedDocument = documentRepository.saveAndFlush(document);

        List<String> viewableUserIds = new ArrayList<>();
        viewableUserIds.add(savedDocument.getCreatedBy());
        // kafka 메시지 발행
        DocumentKafkaDto documentKafkaDto = DocumentKafkaDto.builder()
                .eventType("DOCUMENT_CREATED")
                .eventPayload(DocumentKafkaDto.EventPayload.builder()
                        .id(savedDocument.getId())
                        .createdBy(savedDocument.getCreatedBy())
                        .searchTitle(savedDocument.getTitle())
                        .createdAt(savedDocument.getCreatedAt())
                        .viewableUserIds(viewableUserIds)
                        .build())
                .build();
        try {
            // 3. DTO를 JSON 문자열로 변환
            String message = objectMapper.writeValueAsString(documentKafkaDto);

            // 4. Kafka 토픽으로 이벤트 발행
            kafkaTemplate.send("document-topic", message);

        } catch (JsonProcessingException e) {
            // 예외 처리 (심각한 경우 트랜잭션 롤백 고려)
            throw new RuntimeException("Kafka 메시지 직렬화 실패", e);
        }
        return savedDocument.getId();
    }

    // 문서 삭제(소프트)
    public String deleteDocument(String documentId){
        Document document = documentRepository.findById(documentId).orElseThrow(()->new EntityNotFoundException("해당 문서가 존재하지 않습니다."));
        document.updateIsDelete();
        return document.getTitle();
    }

    // 문서 조회
    @Transactional(readOnly = true)
    public DocumentResDto findDocument(String userId, String documentId){
        Document document = documentRepository.findById(documentId).orElseThrow(()->new EntityNotFoundException(("해당 문서가 존재하지 않습니다.")));
        String getFolderName = null;
        if(document.getFolder()!=null){
            getFolderName = document.getFolder().getName();
        }
        return DocumentResDto.builder()
                .title(document.getTitle())
                .folderName(getFolderName)
                .build();
    }

    // 문서 이름 변경
    public String updateDocument(String documentId, DocumentUpdateDto documentUpdateDto){
        Document document = documentRepository.findById(documentId).orElseThrow(()->new EntityNotFoundException(("해당 문서가 존재하지 않습니다.")));
        if(documentRepository.findByFolderAndTitleAndIsDeleteFalse(document.getFolder(), documentUpdateDto.getTitle()).isPresent()){
            throw new IllegalArgumentException("같은 이름의 파일이 존재합니다.");
        }
        document.updateTitle(documentUpdateDto.getTitle());
        return document.getTitle();
    }

    // 파일 이름 변경
    public String updateFile(String fileId, FileUpdateDto fileUpdateDto){
        File file = fileRepository.findById(fileId).orElseThrow(()->new EntityNotFoundException(("해당 파일이 존재하지 않습니다.")));
        if(fileRepository.findByFolderAndNameAndIsDeleteFalse(file.getFolder(), fileUpdateDto.getName()).isPresent()){
            throw new IllegalArgumentException("같은 이름의 파일이 존재합니다.");
        }
        file.updateName(fileUpdateDto.getName());
        return file.getName();
    }

    public Long getFilesSize(String workspaceId){
        Long totalSize = fileRepository.findTotalSizeByWorkspaceId(workspaceId);
        return (totalSize != null) ? totalSize : 0L;
    }
    
    // 루트 하위 폴더 불러오기
    public List<FolderResDto> getRootFolders(String rootId, String rootType){
        List<FolderResDto> folderResDtos = new ArrayList<>();
        List<Folder> folders = folderRepository.findAllByParentIdIsNullAndRootTypeAndRootIdAndIsDeleteIsFalse(RootType.valueOf(rootType),rootId);
        for(Folder folder : folders){
            folderResDtos.add(FolderResDto.builder()
                    .folderName(folder.getName())
                    .folderId(folder.getId())
                    .build());
        }
        return folderResDtos;
    }

    // 폴더 하위 폴더 불러오기
    public List<FolderResDto> getFolders(String folderId){
        List<FolderResDto> folderResDtos = new ArrayList<>();
        List<Folder> folders = folderRepository.findAllByParentIdAndIsDeleteIsFalse(folderId);
        for(Folder folder : folders){
            folderResDtos.add(FolderResDto.builder()
                    .folderName(folder.getName())
                    .folderId(folder.getId())
                    .build());
        }
        return folderResDtos;
    }
}
