package com.Dolmeng_E.drive.domain.drive.service;

import com.Dolmeng_E.drive.common.controller.WorkspaceServiceClient;
import com.Dolmeng_E.drive.common.dto.EntityNameReqDto;
import com.Dolmeng_E.drive.common.dto.StoneTaskResDto;
import com.Dolmeng_E.drive.common.dto.SubProjectResDto;
import com.Dolmeng_E.drive.common.dto.WorkspaceOrProjectManagerCheckDto;
import com.Dolmeng_E.drive.common.service.S3Uploader;
import com.Dolmeng_E.drive.domain.drive.dto.*;
import com.Dolmeng_E.drive.domain.drive.entity.Document;
import com.Dolmeng_E.drive.domain.drive.entity.File;
import com.Dolmeng_E.drive.domain.drive.entity.Folder;
import com.Dolmeng_E.drive.domain.drive.entity.RootType;
import com.Dolmeng_E.drive.domain.drive.repository.DocumentLineRepository;
import com.Dolmeng_E.drive.domain.drive.repository.DocumentRepository;
import com.Dolmeng_E.drive.domain.drive.repository.FileRepository;
import com.Dolmeng_E.drive.domain.drive.repository.FolderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
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
    private final Tika tika = new Tika();
    private final DocumentLineRepository documentLineRepository;

    public DriverService(FolderRepository folderRepository, S3Uploader s3Uploader, FileRepository fileRepository, DocumentRepository documentRepository, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, @Qualifier("userInventory") RedisTemplate<String, String> redisTemplate, WorkspaceServiceClient workspaceServiceClient, DocumentLineRepository documentLineRepository) {
        this.folderRepository = folderRepository;
        this.s3Uploader = s3Uploader;
        this.fileRepository = fileRepository;
        this.documentRepository = documentRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.hashOperations = redisTemplate.opsForHash();
        this.workspaceServiceClient = workspaceServiceClient;
        this.documentLineRepository = documentLineRepository;
    }

    private static final Set<String> PARSEABLE_MIME_TYPES = Set.of(
            "application/pdf",
            "application/msword", // .doc
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
            "application/vnd.ms-excel", // .xls
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx
            "application/vnd.ms-powerpoint", // .ppt
            "application/vnd.openxmlformats-officedocument.presentationml.presentation", // .pptx
            "text/plain", // .txt
            "text/csv",
            "text/html"
            // (hwp 등 필요한 문서 타입이 있다면 MIME 타입을 확인하여 추가)
    );

    // 폴더 생성
    public String createFolder(FolderSaveDto folderSaveDto, String userId){
        if(folderRepository.findByParentIdAndNameAndIsDeleteIsFalseAndRootId(folderSaveDto.getParentId(), folderSaveDto.getName(), folderSaveDto.getRootId()).isPresent()){
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
                            .rootType(childfolder.getRootType().toString())
                            .rootId(childfolder.getRootId())
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
                    .rootId(file.getRootId())
                    .rootType(file.getRootType().toString())
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
                    .rootType(document.getRootType().toString())
                    .rootId(document.getRootId())
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
                    .rootId(folder.getRootId())
                    .rootType(folder.getRootType().toString())
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
                    .rootType(file.getRootType().toString())
                    .rootId(file.getRootId())
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
                    .rootId(document.getRootId())
                    .rootType(document.getRootType().toString())
                    .build());
        }
        return rootContentsDtos;
    }

    // 폴더 옮기기
    public String updateFolderStruct(String workspaceId, String userId, String folderId, FolderMoveDto folderMoveDto){
        Folder folder = folderRepository.findById(folderId).orElseThrow(()->new EntityNotFoundException("해당 폴더는 존재하지 않습니다"));
        if(folderRepository.findByParentIdAndNameAndIsDeleteIsFalseAndRootId(folderMoveDto.getParentId(), folder.getName(), folderMoveDto.getRootId()).isPresent()){
            throw new IllegalArgumentException("중복된 폴더명입니다.");
        }
        try {
            if(folderMoveDto.getRootType().equals("PROJECT")){
                checkProject(workspaceId, folderMoveDto.getRootId(), userId);
            }else if(folderMoveDto.getRootType().equals("STONE")){
                checkStone(folderMoveDto.getRootId(), userId);
            }
        }catch (Exception e){
            throw new IllegalArgumentException("권한이 없습니다.");
        }
        folder.updateParentId(folderMoveDto.getParentId());
        folder.setRootId(folderMoveDto.getRootId());
        folder.setRootType(RootType.valueOf(folderMoveDto.getRootType()));
        return folder.getParentId();
    }

    // 파일/문서 옮기기
    public String updateElementStruct(String workspaceId, String userId, String elementId, ElementMoveDto elementMoveDto){
        Optional<Folder> folder = Optional.empty();
        if(elementMoveDto.getFolderId()!=null){
            folder = folderRepository.findById(elementMoveDto.getFolderId());
        }
        try {
            if(elementMoveDto.getRootType().equals("PROJECT")){
                checkProject(workspaceId, elementMoveDto.getRootId(), userId);
            }else if(elementMoveDto.getRootType().equals("STONE")){
                checkStone(elementMoveDto.getRootId(), userId);
            }
        }catch (Exception e){
            throw new IllegalArgumentException("권한이 없거나 예상치 못한 오류가 발생하였습니다.");
        }

        if(elementMoveDto.getType().equals("document")){
            Document document = documentRepository.findById(elementId).orElseThrow(()->new EntityNotFoundException("해당 문서가 존재하지 않습니다."));
            if(documentRepository.findByFolderAndTitleAndIsDeleteFalseAndRootId(folder.orElse(null), document.getTitle(), elementMoveDto.getRootId()).isPresent()){
                throw new IllegalArgumentException("중복된 문서가 존재힙니다.");
            };
            document.updateFolder(folder.orElse(null));
            document.setRootId(elementMoveDto.getRootId());
            document.setRootType(RootType.valueOf(elementMoveDto.getRootType()));

            // kafka 메시지 발행
            DocumentKafkaUpdateDto documentKafkaUpdateDto = DocumentKafkaUpdateDto.builder()
                    .eventType("DOCUMENT_UPDATED")
                    .eventPayload(DocumentKafkaUpdateDto.EventPayload.builder()
                            .id(document.getId())
                            .parentId(elementMoveDto.getFolderId() != null ? elementMoveDto.getFolderId() : null)
                            .rootId(document.getRootId())
                            .rootType(document.getRootType().toString())
                            .build())
                    .build();
            try {
                // 3. DTO를 JSON 문자열로 변환
                String message = objectMapper.writeValueAsString(documentKafkaUpdateDto);

                // 4. Kafka 토픽으로 이벤트 발행
                kafkaTemplate.send("document-topic", message);

            } catch (JsonProcessingException e) {
                // 예외 처리 (심각한 경우 트랜잭션 롤백 고려)
                throw new RuntimeException("Kafka 메시지 직렬화 실패", e);
            }
            return document.getTitle();
        }
        else if(elementMoveDto.getType().equals("file")){
            File file = fileRepository.findById(elementId).orElseThrow(()->new EntityNotFoundException("해당 파일이 존재하지 않습니다."));
            if(fileRepository.findByFolderAndNameAndIsDeleteFalseAndRootId(folder.orElse(null), file.getName(), elementMoveDto.getRootId()).isPresent()){
                throw new IllegalArgumentException("중복된 문서가 존재힙니다.");
            };
            file.updateFolder(folder.orElse(null));
            file.setRootId(elementMoveDto.getRootId());
            file.setRootType(RootType.valueOf(elementMoveDto.getRootType()));
            // kafka 메시지 발행
            DocumentKafkaUpdateDto documentKafkaUpdateDto = DocumentKafkaUpdateDto.builder()
                    .eventType("FILE_UPDATED")
                    .eventPayload(DocumentKafkaUpdateDto.EventPayload.builder()
                            .id(file.getId())
                            .parentId(elementMoveDto.getFolderId() != null ? elementMoveDto.getFolderId() : null)
                            .rootId(file.getRootId())
                            .rootType(file.getRootType().toString())
                            .build())
                    .build();
            try {
                // 3. DTO를 JSON 문자열로 변환
                String message = objectMapper.writeValueAsString(documentKafkaUpdateDto);

                // 4. Kafka 토픽으로 이벤트 발행
                kafkaTemplate.send("file-topic", message);

            } catch (JsonProcessingException e) {
                // 예외 처리 (심각한 경우 트랜잭션 롤백 고려)
                throw new RuntimeException("Kafka 메시지 직렬화 실패", e);
            }
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
                if(fileRepository.findByFolderAndNameAndIsDeleteFalseAndRootId(folder.get(), file.getOriginalFilename(), fileSaveDto.getRootId()).isPresent()){
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
                    .size(file.getSize())
                    .build();
            File savedFile = fileRepository.saveAndFlush(fileEntity);

            String extractedContent = extractText(file);
            // kafka 메시지 발행
            List<String> viewableUserIds = new ArrayList<>();
            viewableUserIds.add(savedFile.getCreatedBy());
            DocumentKafkaSaveDto documentKafkaSaveDto = DocumentKafkaSaveDto.builder()
                    .eventType("FILE_CREATED")
                    .eventPayload(DocumentKafkaSaveDto.EventPayload.builder()
                            .id(savedFile.getId())
                            .createdBy(savedFile.getCreatedBy())
                            .searchTitle(savedFile.getName())
                            .createdAt(savedFile.getCreatedAt())
                            .rootId(savedFile.getRootId())
                            .rootType(savedFile.getRootType().toString())
                            .viewableUserIds(viewableUserIds)
                            .searchContent(extractedContent)
                            .fileUrl(savedFile.getUrl())
                            .parentId(savedFile.getFolder()!=null?savedFile.getFolder().getId():null)
                            .size(savedFile.getSize())
                            .build())
                    .build();
            try {
                // 3. DTO를 JSON 문자열로 변환
                String message = objectMapper.writeValueAsString(documentKafkaSaveDto);

                // 4. Kafka 토픽으로 이벤트 발행
                kafkaTemplate.send("file-topic", message);

            } catch (JsonProcessingException e) {
                // 예외 처리 (심각한 경우 트랜잭션 롤백 고려)
                throw new RuntimeException("Kafka 메시지 직렬화 실패", e);
            }
        }

        return "파일 업로드 성공";
    }

    // 파일 내용 추출
    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String mimeType = file.getContentType();
        if (mimeType == null || !PARSEABLE_MIME_TYPES.contains(mimeType)) {
            // "image/jpeg", "video/mp4" 등은 여기서 걸러짐
            return null; // 문서 타입이 아니면 null 반환
        }
        try (InputStream stream = file.getInputStream()) {
            return tika.parseToString(stream);
        } catch (Exception e) {
            // Tika 파싱 중 에러 발생 시
            System.err.println("Tika 파싱 실패 (문서 타입: " + mimeType + "): " + e.getMessage());
            return null;
        }
    }

    // 파일 삭제(소프트)
    public String deleteFile(String fileId){
        File file = fileRepository.findById(fileId).orElseThrow(()->new EntityNotFoundException("파일이 존재하지 않습니다."));
        String fileName = file.getName();
        s3Uploader.delete(file.getUrl());
        file.updateIsDelete();
        // kafka 메시지 발행
        DocumentKafkaUpdateDto documentKafkaUpdateDto = DocumentKafkaUpdateDto.builder()
                .eventType("FILE_DELETED")
                .eventPayload(DocumentKafkaUpdateDto.EventPayload.builder()
                        .id(file.getId())
                        .build())
                .build();
        try {
            // 3. DTO를 JSON 문자열로 변환
            String message = objectMapper.writeValueAsString(documentKafkaUpdateDto);

            // 4. Kafka 토픽으로 이벤트 발행
            kafkaTemplate.send("file-topic", message);

        } catch (JsonProcessingException e) {
            // 예외 처리 (심각한 경우 트랜잭션 롤백 고려)
            throw new RuntimeException("Kafka 메시지 직렬화 실패", e);
        }
        return fileName;
    }

    // 문서 생성
    public String createDocument(String userId, String folderId, DocumentSaveDto documentSaveDto){
        Optional<Folder> folder = folderRepository.findById(folderId);
        // 폴더가 있을 경우
        if(folder.isPresent()){
            if(documentRepository.findByFolderAndTitleAndIsDeleteFalseAndRootId(folder.get(), documentSaveDto.getName(), documentSaveDto.getRootId()).isPresent()){
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
        // kafka 메시지 발행
        List<String> viewableUserIds = new ArrayList<>();
        viewableUserIds.add(savedDocument.getCreatedBy());
        DocumentKafkaSaveDto documentKafkaSaveDto = DocumentKafkaSaveDto.builder()
                .eventType("DOCUMENT_CREATED")
                .eventPayload(DocumentKafkaSaveDto.EventPayload.builder()
                        .id(savedDocument.getId())
                        .createdBy(savedDocument.getCreatedBy())
                        .searchTitle(savedDocument.getTitle())
                        .createdAt(savedDocument.getCreatedAt())
                        .rootId(savedDocument.getRootId())
                        .rootType(savedDocument.getRootType().toString())
                        .viewableUserIds(viewableUserIds)
                        .parentId(savedDocument.getFolder() != null ? savedDocument.getFolder().getId() : null)
                        .build())
                .build();
        try {
            // 3. DTO를 JSON 문자열로 변환
            String message = objectMapper.writeValueAsString(documentKafkaSaveDto);

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
        documentLineRepository.deleteAllByDocument(document);
        document.updateIsDelete();
        // kafka 메시지 발행
        DocumentKafkaUpdateDto documentKafkaUpdateDto = DocumentKafkaUpdateDto.builder()
                .eventType("DOCUMENT_DELETED")
                .eventPayload(DocumentKafkaUpdateDto.EventPayload.builder()
                        .id(document.getId())
                        .build())
                .build();
        try {
            // 3. DTO를 JSON 문자열로 변환
            String message = objectMapper.writeValueAsString(documentKafkaUpdateDto);

            // 4. Kafka 토픽으로 이벤트 발행
            kafkaTemplate.send("document-topic", message);

        } catch (JsonProcessingException e) {
            // 예외 처리 (심각한 경우 트랜잭션 롤백 고려)
            throw new RuntimeException("Kafka 메시지 직렬화 실패", e);
        }
        return document.getTitle();
    }

    // 문서 조회
    @Transactional(readOnly = true)
    public DocumentResDto findDocument(String userId, String documentId, String workspaceId){
        Document document = documentRepository.findById(documentId).orElseThrow(()->new EntityNotFoundException(("해당 문서가 존재하지 않습니다.")));
        if(document.getRootType().toString().equals("PROJECT")){
            try {
                if (Objects.requireNonNull(workspaceServiceClient.checkProjectMembership(document.getRootId(), userId).getBody()).getResult().equals(false)
                        &&Objects.requireNonNull(workspaceServiceClient.checkWorkspaceManager(workspaceId, userId).getBody()).getResult().equals(false)){
                    throw new IllegalArgumentException("권한이 없습니다.");
                }
            }catch (FeignException e){
                System.out.println(e.getMessage());
                throw new IllegalArgumentException("예상치못한오류 발생");
            }
        }else if(document.getRootType().toString().equals("STONE")){
            try {
                WorkspaceOrProjectManagerCheckDto workspaceOrProjectManagerCheckDto = workspaceServiceClient.checkWorkspaceOrProjectManager(document.getRootId(), userId);
                if(Objects.requireNonNull(workspaceServiceClient.checkStoneMembership(document.getRootId(), userId).getBody()).getResult().equals(false)
                        && !workspaceOrProjectManagerCheckDto.isProjectManager() && !workspaceOrProjectManagerCheckDto.isWorkspaceManager()
                        && Objects.requireNonNull(workspaceServiceClient.checkStoneManagership(document.getRootId(), userId).getBody()).getResult().equals(false)
                ){
                    throw new IllegalArgumentException("권한이 없습니다.");
                }
            }catch (FeignException e){
                System.out.println(e.getMessage());
                throw new IllegalArgumentException("예상치못한오류 발생");
            }
        }
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
        // kafka 메시지 발행
        DocumentKafkaUpdateDto documentKafkaUpdateDto = DocumentKafkaUpdateDto.builder()
                .eventType("DOCUMENT_UPDATED")
                .eventPayload(DocumentKafkaUpdateDto.EventPayload.builder()
                        .id(document.getId())
                        .searchTitle(documentUpdateDto.getTitle())
                        .parentId(document.getFolder() != null ? document.getFolder().getId() : null)
                        .build())
                .build();
        try {
            // 3. DTO를 JSON 문자열로 변환
            String message = objectMapper.writeValueAsString(documentKafkaUpdateDto);

            // 4. Kafka 토픽으로 이벤트 발행
            kafkaTemplate.send("document-topic", message);

        } catch (JsonProcessingException e) {
            // 예외 처리 (심각한 경우 트랜잭션 롤백 고려)
            throw new RuntimeException("Kafka 메시지 직렬화 실패", e);
        }
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
    public List<FolderResDto> getRootFolders(String workspaceId, String userId, String rootId, String rootType){
        List<FolderResDto> folderResDtos = new ArrayList<>();
        if(rootType.equals("WORKSPACE")){
            try {
                List<SubProjectResDto> subProjectResDtos = workspaceServiceClient.getSubProjectsByWorkspace(rootId);
                for(SubProjectResDto subProjectResDto : subProjectResDtos){
                    folderResDtos.add(FolderResDto.builder()
                            .rootType("PROJECT")
                            .rootId(subProjectResDto.getProjectId())
                            .rootName(subProjectResDto.getProjectName())
                            .build());
                }
            }catch (FeignException e){
                System.out.println(e.getMessage());
                throw new IllegalArgumentException("예상치못한오류 발생");
            }
        }else if(rootType.equals("PROJECT")) {
            try {
                if (Objects.requireNonNull(workspaceServiceClient.checkProjectMembership(rootId, userId).getBody()).getResult().equals(false)
                        && Objects.requireNonNull(workspaceServiceClient.checkWorkspaceManager(workspaceId, userId).getBody()).getResult().equals(false)) {
                    throw new IllegalArgumentException("권한이 없습니다.");
                }
                List<StoneTaskResDto.StoneInfo> stoneInfos = workspaceServiceClient.getSubStonesAndTasks(rootId).getStones();
                for(StoneTaskResDto.StoneInfo stoneInfo : stoneInfos){
                    folderResDtos.add(FolderResDto.builder()
                            .rootType("STONE")
                            .rootId(stoneInfo.getStoneId())
                            .rootName(stoneInfo.getStoneName())
                            .build());
                }

            } catch (FeignException e) {
                System.out.println(e.getMessage());
                throw new IllegalArgumentException("예상치못한오류 발생");
            }
        }else if (rootType.equals("STONE")) {
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
            folderResDtos.add(FolderResDto.builder()
                    .folderName(folder.getName())
                    .folderId(folder.getId())
                    .rootId(folder.getRootId())
                    .rootType(folder.getRootType().toString())
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

    public String getRootName(String userId, String rootId, String rootType){
        if(rootType.equals("WORKSPACE")){
            return workspaceServiceClient.getEntityName(userId, EntityNameReqDto.builder()
                    .workspaceId(rootId).build()).getName();
        }
        else if(rootType.equals("STONE")){
            return workspaceServiceClient.getEntityName(userId, EntityNameReqDto.builder()
                    .stoneId(rootId).build()).getName();
        }
        else{
            return workspaceServiceClient.getEntityName(userId, EntityNameReqDto.builder()
                    .projectId(rootId).build()).getName();
        }
    }

    // 문서 상세 정보 
    public DocumentInfoPage getDocumentInfoPage(String documentId){
        Document document = documentRepository.findById(documentId).orElseThrow(()->new EntityNotFoundException("존재하지 않은 문서입니다."));
        Map<String, String> userInfo = hashOperations.entries("user:"+document.getCreatedBy());
        return DocumentInfoPage.builder()
                .name(document.getTitle())
                .creatorName(userInfo.get("name"))
                .createdBy(document.getCreatedBy())
                .folderName((document.getFolder() != null) ? document.getFolder().getName() : "최상위 문서")
                .updatedAt(document.getUpdatedAt())
                .createdAt(document.getCreatedAt())
                .build();
    }
    
    // 폴더 상세 정보
    public FolderInfoPage getFolderInfoPage(String folderId){
        Folder folder = folderRepository.findById(folderId).orElseThrow(()->new EntityNotFoundException("존재하지 않은 폴더입니다."));
        Map<String, String> userInfo = hashOperations.entries("user:"+folder.getCreatedBy());
        Folder parentFolder = null;
        if(folder.getParentId()!=null){
            parentFolder = folderRepository.findById(folder.getParentId()).orElseThrow(()->new EntityNotFoundException("상위 폴더가 존재하지 않습니다."));
        }
        return FolderInfoPage.builder()
                .name(folder.getName())
                .creatorName(userInfo.get("name"))
                .createdBy(folder.getCreatedBy())
                .parentFolderName((parentFolder != null) ? parentFolder.getName() : "최상위 폴더")
                .updatedAt(folder.getUpdatedAt())
                .createdAt(folder.getCreatedAt())
                .build();
    }

    // 파일 상세 정보
    public FileInfoPage getFileInfoPage(String fileId){
        File file = fileRepository.findById(fileId).orElseThrow(()->new EntityNotFoundException("존재하지 않은 파일입니다."));
        Map<String, String> userInfo = hashOperations.entries("user:"+file.getCreatedBy());
        return FileInfoPage.builder()
                .name(file.getName())
                .creatorName(userInfo.get("name"))
                .createdBy(file.getCreatedBy())
                .folderName((file.getFolder() != null) ? file.getFolder().getName() : "최상위 파일")
                .updatedAt(file.getUpdatedAt())
                .createdAt(file.getCreatedAt())
                .fileSize(file.getSize())
                .build();
    }
    public void checkProject(String workspaceId, String rootId ,String userId){
        try {
            if (Objects.requireNonNull(workspaceServiceClient.checkProjectMembership(rootId, userId).getBody()).getResult().equals(false)
                    &&Objects.requireNonNull(workspaceServiceClient.checkWorkspaceManager(workspaceId, userId).getBody()).getResult().equals(false)){
                throw new IllegalArgumentException("권한이 없습니다.");
            }
        }catch (FeignException e){
            System.out.println(e.getMessage());
            throw new IllegalArgumentException("예상치못한오류 발생");
        }
    }
    public void checkStone(String rootId, String userId){
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
    
    // 루트id와 type의 모든 폴더/파일/문서 삭제
    public void deleteAll(String rootId, String rootType){
        if(rootType.equals("WORKSPACE")){
            documentRepository.softDeleteByRootInfo(RootType.valueOf(rootType), rootId);
            fileRepository.softDeleteByRootInfo(RootType.valueOf(rootType), rootId);
            folderRepository.softDeleteByRootInfo(RootType.valueOf(rootType), rootId);
            List<SubProjectResDto> subProjectResDtos = workspaceServiceClient.getSubProjectsByWorkspace(rootId);
            for(SubProjectResDto subProjectResDto : subProjectResDtos){
                documentRepository.softDeleteByRootInfo(RootType.PROJECT, subProjectResDto.getProjectId());
                fileRepository.softDeleteByRootInfo(RootType.PROJECT, subProjectResDto.getProjectId());
                folderRepository.softDeleteByRootInfo(RootType.PROJECT, subProjectResDto.getProjectId());
                List<StoneTaskResDto.StoneInfo> stoneInfos = workspaceServiceClient.getSubStonesAndTasks(subProjectResDto.getProjectId()).getStones();
                for(StoneTaskResDto.StoneInfo stoneInfo : stoneInfos){
                    documentRepository.softDeleteByRootInfo(RootType.STONE, stoneInfo.getStoneId());
                    fileRepository.softDeleteByRootInfo(RootType.STONE, stoneInfo.getStoneId());
                    folderRepository.softDeleteByRootInfo(RootType.STONE, stoneInfo.getStoneId());
                }
            }
        }else if(rootType.equals("STONE")){
            documentRepository.softDeleteByRootInfo(RootType.valueOf(rootType), rootId);
            fileRepository.softDeleteByRootInfo(RootType.valueOf(rootType), rootId);
            folderRepository.softDeleteByRootInfo(RootType.valueOf(rootType), rootId);
        }else if(rootType.equals("PROJECT")){
            documentRepository.softDeleteByRootInfo(RootType.valueOf(rootType), rootId);
            fileRepository.softDeleteByRootInfo(RootType.valueOf(rootType), rootId);
            folderRepository.softDeleteByRootInfo(RootType.valueOf(rootType), rootId);
            List<StoneTaskResDto.StoneInfo> stoneInfos = workspaceServiceClient.getSubStonesAndTasks(rootId).getStones();
            for(StoneTaskResDto.StoneInfo stoneInfo : stoneInfos){
                documentRepository.softDeleteByRootInfo(RootType.STONE, stoneInfo.getStoneId());
                fileRepository.softDeleteByRootInfo(RootType.STONE, stoneInfo.getStoneId());
                folderRepository.softDeleteByRootInfo(RootType.STONE, stoneInfo.getStoneId());
            }
        }
    }
}
