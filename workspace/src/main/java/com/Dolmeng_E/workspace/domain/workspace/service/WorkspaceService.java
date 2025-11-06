package com.Dolmeng_E.workspace.domain.workspace.service;

import com.Dolmeng_E.workspace.common.controller.DriveServiceClient;
import com.Dolmeng_E.workspace.common.controller.SearchServiceClient;
import com.Dolmeng_E.workspace.common.dto.*;
import com.Dolmeng_E.workspace.common.service.UserFeign;
import com.Dolmeng_E.workspace.domain.access_group.dto.AccessGroupAddUserDto;
import com.Dolmeng_E.workspace.domain.access_group.entity.AccessGroup;
import com.Dolmeng_E.workspace.domain.access_group.repository.AccessGroupRepository;
import com.Dolmeng_E.workspace.domain.access_group.service.AccessGroupService;
import com.Dolmeng_E.workspace.domain.project.dto.ProjectProgressResDto;
import com.Dolmeng_E.workspace.domain.project.entity.Project;
import com.Dolmeng_E.workspace.domain.project.entity.ProjectParticipant;
import com.Dolmeng_E.workspace.domain.project.repository.ProjectParticipantRepository;
import com.Dolmeng_E.workspace.domain.project.repository.ProjectRepository;
import com.Dolmeng_E.workspace.domain.stone.dto.MilestoneResDto;
import com.Dolmeng_E.workspace.domain.stone.dto.ProjectMilestoneResDto;
import com.Dolmeng_E.workspace.domain.stone.dto.StoneKafkaViewableUpdateDto;
import com.Dolmeng_E.workspace.domain.stone.entity.ChildStoneList;
import com.Dolmeng_E.workspace.domain.stone.entity.Stone;
import com.Dolmeng_E.workspace.domain.stone.entity.StoneParticipant;
import com.Dolmeng_E.workspace.domain.stone.repository.StoneParticipantRepository;
import com.Dolmeng_E.workspace.domain.stone.repository.StoneRepository;
import com.Dolmeng_E.workspace.domain.task.entity.Task;
import com.Dolmeng_E.workspace.domain.task.repository.TaskRepository;
import com.Dolmeng_E.workspace.domain.user_group.dto.UserGroupAddUserDto;
import com.Dolmeng_E.workspace.domain.user_group.entity.UserGroup;
import com.Dolmeng_E.workspace.domain.user_group.entity.UserGroupMapping;
import com.Dolmeng_E.workspace.domain.user_group.repository.UserGroupMappingRepository;
import com.Dolmeng_E.workspace.domain.user_group.repository.UserGroupRepository;
import com.Dolmeng_E.workspace.domain.user_group.service.UserGroupService;
import com.Dolmeng_E.workspace.domain.workspace.dto.*;
import com.Dolmeng_E.workspace.domain.workspace.entity.*;
import com.Dolmeng_E.workspace.domain.workspace.repository.WorkspaceInviteRepository;
import com.Dolmeng_E.workspace.domain.workspace.repository.WorkspaceParticipantRepository;
import com.Dolmeng_E.workspace.domain.workspace.repository.WorkspaceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class WorkspaceService {
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceParticipantRepository workspaceParticipantRepository;
    private final UserFeign userFeign;
    private final AccessGroupService accessGroupService;
    private final AccessGroupRepository accessGroupRepository;
    private final WorkspaceInviteRepository workspaceInviteRepository;
    private final ProjectParticipantRepository projectParticipantRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserGroupMappingRepository userGroupMappingRepository;
    private final StoneParticipantRepository stoneParticipantRepository;
    private final StoneRepository stoneRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final DriveServiceClient driveServiceClient;
    private final SearchServiceClient searchServiceClient;

    //    워크스페이스 생성(PRO,ENTERPRISE)
    public String createWorkspace(WorkspaceCreateDto workspaceCreateDto, String userId) {


        // 1. 워크스페이스 생성
        UserInfoResDto userInfoResDto = userFeign.fetchUserInfoById(userId);
        Workspace workspace = workspaceCreateDto.toEntity(userInfoResDto.getUserId());
        workspace.settingMaxStorage(workspaceCreateDto.getWorkspaceTemplates());
        workspaceRepository.save(workspace);

        // 2. 워크스페이스 관리자 권한그룹 생성
        String adminAccessGroupId = accessGroupService.createAdminGroupForWorkspace(workspace.getId());

        // 3. 워크스페이스 일반 사용자 권한그룹 생성
        accessGroupService.createDefaultUserAccessGroup(workspace.getId());

        // 4. 워크스페이스 참여자에 추가
        AccessGroup adminAccessGroup = accessGroupRepository.findById(adminAccessGroupId)
                .orElseThrow(()->new EntityNotFoundException("해당 관리자 그룹 없습니다."));
        workspaceParticipantRepository.save(
                WorkspaceParticipant.builder()
                .workspaceRole(WorkspaceRole.ADMIN)
                .accessGroup(adminAccessGroup)
                .userId(userInfoResDto.getUserId())
                .isDelete(false)
                .workspace(workspace)
                .userName(userInfoResDto.getUserName())
                .build()
        );
        return workspace.getId();
    }


//  회원생성시 받은 메세지를 받아 dto를 조립하여 createPersonalWorkspace() 를 실행하는 메서드(보상트랜잭션 분기처리용)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean tryCreatePersonalWorkspace(UserSignupEventDto event) {
        try {
            PersonalWorkspaceCreateDto dto = PersonalWorkspaceCreateDto.builder()
                    .workspaceName(event.getUserName() + "의 워크스페이스")
                    .userId(UUID.fromString(event.getUserId()))
                    .workspaceTemplates(WorkspaceTemplates.PERSONAL)
                    .userName(event.getUserName())
                    .build();

            createPersonalWorkspace(dto);
            return true;

        } catch (Exception e) {
            log.error("워크스페이스 생성 실패: {}", e.getMessage());
            return false;
        }
    }

    // 개인 워크스페이스 생성 (회원가입 시 자동 생성용, 컨트롤러 호출 X)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void createPersonalWorkspace(PersonalWorkspaceCreateDto dto) {
        try {
            // 1. 워크스페이스 생성
            Workspace workspace = dto.toEntity();
            workspace.settingMaxStorage(WorkspaceTemplates.PERSONAL);
            workspaceRepository.save(workspace);

            // 2. 워크스페이스 관리자 권한그룹 생성
            String adminAccessGroupId = accessGroupService.createAdminGroupForWorkspace(workspace.getId());

//            // 보상 트랜잭션 테스트용 예외 발생 지점
//            if (true) {
//                throw new IllegalArgumentException("보상트랜잭션용 에러 (테스트)");
//            }

            // 3. 워크스페이스 일반 사용자 권한그룹 생성
            accessGroupService.createDefaultUserAccessGroup(workspace.getId());

            // 4. 워크스페이스 참여자 추가
            AccessGroup adminAccessGroup = accessGroupRepository.findById(adminAccessGroupId)
                    .orElseThrow(() -> new EntityNotFoundException("해당 관리자 그룹이 없습니다."));
            workspaceParticipantRepository.save(
                    WorkspaceParticipant.builder()
                            .workspaceRole(WorkspaceRole.ADMIN)
                            .accessGroup(adminAccessGroup)
                            .userId(dto.getUserId())
                            .isDelete(false)
                            .workspace(workspace)
                            .userName(dto.getUserName())
                            .build()
            );

            log.info("개인 워크스페이스 생성 완료: {}", workspace.getWorkspaceName());

        } catch (Exception e) {
            // 이 catch 블록에서 잡힌 예외를 다시 던져야
            // UserSignupConsumer의 try-catch에서 감지 가능함
            log.error("워크스페이스 생성 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("워크스페이스 생성 실패", e);
        }
    }



//    워크스페이스 목록 조회
    @Transactional(readOnly = true)
    public List<WorkspaceListResDto> getWorkspaceList(String userId) {
        // 1. 유저 정보 조회 (Feign)
        UserInfoResDto userInfo = userFeign.fetchUserInfoById(userId);

        // 2. 사용자가 속한 워크스페이스 조회
        List<WorkspaceParticipant> participants =
                workspaceParticipantRepository.findByUserIdAndIsDeleteFalse(userInfo.getUserId());

        // 3. DTO 변환 + 필터링
        return participants.stream()
                .filter(p -> p.getWorkspace() != null && !Boolean.TRUE.equals(p.getWorkspace().getIsDelete())) // 워크스페이스 삭제 제외
                .map(p -> {
                    Workspace workspace = p.getWorkspace();
                    return WorkspaceListResDto.builder()
                            .workspaceId(workspace.getId())
                            .workspaceName(workspace.getWorkspaceName())
                            .workspaceTemplates(workspace.getWorkspaceTemplates())
                            .subscribe(workspace.getSubscribe())
                            .currentStorage(workspace.getCurrentStorage())
                            .maxStorage(workspace.getMaxStorage())
                            .role(p.getWorkspaceRole().name())
                            .build();
                })
                .toList();
    }

//    워크스페이스 상세조회
    @Transactional(readOnly = true)
    public WorkspaceDetailResDto getWorkspaceDetail(String userId, String workspaceId) {
        // 1. 유저 확인
        UserInfoResDto userInfo = userFeign.fetchUserInfoById(userId);

        // 2. 워크스페이스 존재 여부
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("해당 워크스페이스를 찾을 수 없습니다."));

        // 3. 참여자 확인 (접근 권한)
        workspaceParticipantRepository.findByWorkspaceIdAndUserId(workspaceId, userInfo.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("해당 워크스페이스에 접근 권한이 없습니다."));

        // 삭제 여부 확인
        if (Boolean.TRUE.equals(workspace.getIsDelete())) {
            throw new EntityNotFoundException("삭제된 워크스페이스입니다.");
        }

        // 4. 구성원 수
        long memberCount = workspaceParticipantRepository.countByWorkspaceIdAndIsDeleteFalse(workspaceId);

        // 5. 상세 DTO 변환
        return WorkspaceDetailResDto.builder()
                .workspaceId(workspace.getId())
                .workspaceName(workspace.getWorkspaceName())
                .workspaceTemplates(workspace.getWorkspaceTemplates())
                .createdAt(workspace.getCreatedAt())
                .subscribe(workspace.getSubscribe())
                .memberCount(memberCount)
                .currentStorage(workspace.getCurrentStorage())
                .maxStorage(workspace.getMaxStorage())
                .build();
    }

    //    워크스페이스 변경
    @Transactional
    public void updateWorkspaceName(String userId, String workspaceId, WorkspaceNameUpdateDto dto) {
        // 1. 요청자 정보 조회
        UserInfoResDto requester = userFeign.fetchUserInfoById(userId);

        // 2. 워크스페이스 조회
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("해당 워크스페이스가 존재하지 않습니다."));

        // 3. 관리자 권한 확인
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspaceId, requester.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참가자 정보를 찾을 수 없습니다."));

        if (!participant.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            throw new EntityNotFoundException("관리자만 워크스페이스명을 변경할 수 있습니다.");
        }

        // 4. 이름 변경
        workspace.updateWorkspaceName(dto.getWorkspaceName());
    }

    // 워크스페이스 회원 초대
    public void addParticipants(String userId, String workspaceId, WorkspaceAddUserDto dto) {

        // 1. 워크스페이스 확인
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스를 찾을 수 없습니다."));

        // 개인워크스페이스에는 안되게 방어코드
        validateNotPersonalWorkspace(workspace);

        // 2. 요청자 권한 확인
        UserInfoResDto requester = userFeign.fetchUserInfoById(userId);
        WorkspaceParticipant admin = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspaceId, requester.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("요청자 워크스페이스 참가자 정보가 없습니다."));

        if (!admin.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            throw new IllegalArgumentException("관리자만 사용자 추가 가능");
        }

        // 3. 초대할 사용자 목록
        UserIdListDto userIdListDto = new UserIdListDto(dto.getUserIdList());
        UserInfoListResDto userInfoListResDto = userFeign.fetchUserListInfo(userIdListDto);

        List<UserInfoResDto> filteredUserList = userInfoListResDto.getUserInfoList().stream()
                .filter(userInfo -> !userInfo.getUserId().equals(requester.getUserId()))
                .toList();

        if (filteredUserList.isEmpty()) {
            throw new IllegalArgumentException("초대 가능한 사용자가 없습니다. (관리자 본인은 제외됩니다)");
        }

        // 4. 일반 그룹 조회
        AccessGroup commonAccessGroup = accessGroupRepository.findByWorkspaceIdAndAccessGroupName(workspaceId, "일반 유저 그룹")
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 ID 혹은 권한 그룹명에 맞는 정보가 없습니다."));

        List<WorkspaceParticipant> newParticipants = new ArrayList<>();

        for (UserInfoResDto userInfo : filteredUserList) {
            Optional<WorkspaceParticipant> existingOpt =
                    workspaceParticipantRepository.findByWorkspaceIdAndUserId(workspaceId, userInfo.getUserId());

            if (existingOpt.isPresent()) {
                WorkspaceParticipant existing = existingOpt.get();

                // 1️. 이미 존재하고 삭제되지 않은 경우 → 예외 던지기
                if (!existing.isDelete()) {
                    throw new IllegalArgumentException("이미 워크스페이스에 존재하는 사용자입니다: " + userInfo.getUserName());
                }

                // 2️. 존재하지만 삭제된 경우 → 복귀 처리
                // 추가 : 삭제 시 권한그룹을 없앴기 때문에 다시 일반 권한그룹으로 추가
                existing.restoreParticipant();
                AccessGroup accessGroup = accessGroupRepository.findByWorkspaceIdAndAccessGroupName(workspaceId,"일반 유저 그룹")
                        .orElseThrow(()->new EntityNotFoundException("권한그룹이 존재하지 않습니다."));
                existing.setAccessGroup(accessGroup);
                workspaceParticipantRepository.save(existing);
                continue;
            }

            // 3️. 신규 사용자 → 추가
            newParticipants.add(
                    WorkspaceParticipant.builder()
                            .workspace(workspace)
                            .workspaceRole(WorkspaceRole.COMMON)
                            .userId(userInfo.getUserId())
                            .accessGroup(commonAccessGroup)
                            .userName(userInfo.getUserName())
                            .isDelete(false)
                            .build()
            );
        }

        // 5. 신규 사용자 저장
        if (!newParticipants.isEmpty()) {
            workspaceParticipantRepository.saveAll(newParticipants);
            workspaceParticipantRepository.flush();
        }

        // kafka 메시지 발행
        Set<UUID> workspaceNewParticipants = newParticipants.stream()
                .map(WorkspaceParticipant::getUserId)
                .collect(Collectors.toSet());
        StoneKafkaViewableUpdateDto stoneKafkaViewableUpdateDto = StoneKafkaViewableUpdateDto.builder()
                .eventType("WORKSPACE_PARTICIPANT_UPDATE")
                .eventPayload(StoneKafkaViewableUpdateDto.EventPayload.builder()
                        .id(workspace.getId())
                        .userIds(workspaceNewParticipants)
                        .build())
                .build();
        try {
            String message = objectMapper.writeValueAsString(stoneKafkaViewableUpdateDto);
            kafkaTemplate.send("update-viewable-topic", message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Kafka 메시지 직렬화 실패", e);
        }
    }



//    워크스페이스 참여자 목록 조회
    @Transactional(readOnly = true)
    public Page<WorkspaceParticipantResDto> getWorkspaceParticipants(String userId, String workspaceId, Pageable pageable) {

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("해당 워크스페이스를 찾을 수 없습니다."));

        Page<WorkspaceParticipant> participantPage =
                workspaceParticipantRepository.findAllByWorkspaceId(workspaceId, pageable);

        return participantPage.map(p -> WorkspaceParticipantResDto.builder()
                .userId(p.getUserId())
                .userName(p.getUserName())
                .workspaceRole(p.getWorkspaceRole().name())
                .workspaceParticipantId(p.getId())
                .isDeleted(p.isDelete())
                .accessGroupId(p.getAccessGroup() != null ? p.getAccessGroup().getId() : null)
                .accessGroupName(p.getAccessGroup() != null ? p.getAccessGroup().getAccessGroupName() : null)
                .build());
    }



    //    워크스페이스 회원 삭제
    @Transactional
    public void deleteWorkspaceParticipants(String userId, String workspaceId, WorkspaceDeleteUserDto dto) {
        // 1. 관리자 확인
        UserInfoResDto adminInfo = userFeign.fetchUserInfoById(userId);

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("해당 워크스페이스를 찾을 수 없습니다."));

        WorkspaceParticipant admin = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspaceId, adminInfo.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("요청자는 워크스페이스에 존재하지 않습니다."));

        if (!admin.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            throw new IllegalArgumentException("관리자만 회원 삭제가 가능합니다.");
        }

        // 2. 자기 자신 포함 방지
        if (dto.getUserIdList().contains(adminInfo.getUserId())) {
            throw new IllegalArgumentException("관리자는 자기 자신을 삭제할 수 없습니다.");
        }

        // 3. 유저 리스트 순회하면서 논리 삭제 및 그룹 해제 처리
        for (UUID targetUserId : dto.getUserIdList()) {
            WorkspaceParticipant target = workspaceParticipantRepository
                    .findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                    .orElseThrow(() -> new EntityNotFoundException("삭제 대상 사용자를 찾을 수 없습니다."));

            // 사용자 그룹 매핑(UserGroupMapping) 삭제
            userGroupMappingRepository.deleteByWorkspaceParticipant(target);

            // 권한그룹 연결 해제
            target.setAccessGroup(null);

            // 논리 삭제 처리
            if (!target.isDelete()) {
                target.deleteParticipant();
                workspaceParticipantRepository.save(target);
            }
        }
    }

    // 워크스페이스 삭제
    public void deleteWorkspace(String userId, String workspaceId) {
        driveServiceClient.deleteAll("WORKSPACE", workspaceId);
        searchServiceClient.deleteAll("WORKSPACE", workspaceId);
        // 1. 유저 정보 검증 (Feign)
        UserInfoResDto userInfo = userFeign.fetchUserInfoById(userId);

        // 2. 워크스페이스 존재 여부 및 삭제여부 확인
        Workspace workspace = workspaceRepository.findByIdAndIsDeleteFalse(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않거나 이미 삭제된 워크스페이스입니다."));

        // 3. 접근 권한 확인 (참여자인지 체크)
        workspaceParticipantRepository.findByWorkspaceIdAndUserId(workspaceId, userInfo.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("해당 워크스페이스에 접근 권한이 없습니다."));

        // 4. 워크스페이스 관리자인지 체크
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspaceId, userInfo.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("해당 워크스페이스에 접근 권한이 없습니다."));

        if (participant.getWorkspaceRole() != WorkspaceRole.ADMIN) {
            throw new IllegalArgumentException("관리자가 아니면 워크스페이스 삭제 불가능합니다.");
        }

        // 5. 삭제
        workspace.deleteWorkspace();

        // 6. 워크스페이스 참여자들도 함께 비활성화
        List<WorkspaceParticipant> participants = workspaceParticipantRepository.findAllByWorkspace(workspace);
        participants.forEach(p -> p.deleteParticipant());
    }

    // 워크스페이스 정보 반환
    public WorkspaceInfoResDto fetchWorkspaceInfo (String userId, WorkspaceNameDto workspaceName) {
        Workspace workspace = workspaceRepository.findByUserIdAndWorkspaceName(UUID.fromString(userId), workspaceName.getWorkspaceName());

        return WorkspaceInfoResDto.builder()
                .workspaceId(workspace.getId())
                .workspaceName(workspace.getWorkspaceName())
                .build();
    }

    // 워크스페이스 존재 여부 확인
    @Transactional(readOnly = true)
    public boolean existsById(String workspaceId) {
        return workspaceRepository.existsById(workspaceId);
    }

    // 워크스페이스 멤버 여부 확인
    @Transactional(readOnly = true)
    public boolean checkWorkspaceMember(String workspaceId, UUID userId) {
        return workspaceParticipantRepository.existsByWorkspaceIdAndUserId(workspaceId, userId);
    }

//    워크스페이스 전체 프로젝트별 마일스톤 조회
    @Transactional(readOnly = true)
    public List<ProjectProgressResDto> getWorkspaceProjectProgress(String userId, String workspaceId) {

        // 1. 워크스페이스 검증
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스를 찾을 수 없습니다."));

        // 개인워크스페이스에는 안되게 방어코드
        validateNotPersonalWorkspace(workspace);

        // 2. 요청자 검증
        WorkspaceParticipant requester = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspaceId, UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 3. 관리자 권한 검증
        if (!requester.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            throw new IllegalArgumentException("관리자만 접근할 수 있습니다.");
        }

        // 4. 관리자 권한이므로 workspace 전체 프로젝트 조회 (스톤 없어도 다 가져오기)
        List<ProjectParticipant> projectParticipants =
                projectParticipantRepository.findAllWithOptionalStonesByWorkspaceParticipant(requester);

        // 5. DTO 변환용 중복 제거 (동일 프로젝트 중복 방지)
        Set<Project> uniqueProjects = projectParticipants.stream()
                .map(ProjectParticipant::getProject)
                .collect(Collectors.toSet());

        // 6. 마일스톤 최신화 및 DTO 변환
        List<ProjectProgressResDto> result = uniqueProjects.stream()
                .map(project -> {
                    project.updateMilestone();
                    return ProjectProgressResDto.fromEntity(project);
                })
                .toList();

        return result;
    }

    // 워크스페이스 전체 프로젝트별 프로젝트 마일스톤, 스톤 목록과 스톤의 마일스톤들 조회
    @Transactional(readOnly = true)
    public List<ProjectMilestoneResDto> milestoneListForAdmin(String userId, String workspaceId) {

        // 1. 워크스페이스 검증
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스가 존재하지 않습니다."));

        // 개인워크스페이스에는 안되게 방어코드
        validateNotPersonalWorkspace(workspace);

        // 2. 사용자 검증
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspaceId, UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 존재하지 않습니다."));

        // 3. 관리자 권한 검증
        if (participant.getWorkspaceRole() != WorkspaceRole.ADMIN) {
            throw new IllegalArgumentException("관리자만 접근할 수 있습니다.");
        }

        // 4. 프로젝트 + 스톤 목록 조회 (fetch join)
        List<ProjectParticipant> projectParticipants =
                projectParticipantRepository.findAllWithStonesByWorkspaceParticipant(participant);

        // 5. 중복 프로젝트 제거
        Set<Project> uniqueProjects = projectParticipants.stream()
                .map(ProjectParticipant::getProject)
                .collect(Collectors.toSet());

        List<ProjectMilestoneResDto> result = new ArrayList<>();

        // 6. 각 프로젝트별로 스톤 트리 구성
        for (Project project : uniqueProjects) {
            List<Stone> stones = project.getStones().stream()
                    .filter(s -> !s.getIsDelete())
                    .toList();

            // 모든 스톤 → DTO 변환 (중복 무시)
            Map<String, MilestoneResDto> dtoMap = stones.stream()
                    .collect(Collectors.toMap(
                            Stone::getId,
                            MilestoneResDto::fromEntity,
                            (a, b) -> a // 중복 발생 시 첫 번째 유지
                    ));

            // ChildStoneList 기반으로 부모-자식 연결
            for (Stone stone : stones) {
                if (stone.getChildStoneLists() == null) continue;

                for (ChildStoneList link : stone.getChildStoneLists()) {
                    Stone child = link.getChildStone();
                    if (child != null && !child.getIsDelete()) {
                        MilestoneResDto parentDto = dtoMap.get(stone.getId());
                        MilestoneResDto childDto = dtoMap.get(child.getId());
                        if (parentDto != null && childDto != null) {
                            parentDto.getChildren().add(childDto);
                        }
                    }
                }
            }

            // 루트 스톤(= 자식으로 포함되지 않은 스톤)만 추출
            Set<String> childIds = stones.stream()
                    .flatMap(s -> s.getChildStoneLists().stream()
                            .map(c -> c.getChildStone().getId()))
                    .collect(Collectors.toSet());

            List<MilestoneResDto> rootStones = stones.stream()
                    .filter(s -> !childIds.contains(s.getId()))
                    .map(s -> dtoMap.get(s.getId()))
                    .toList();

            // 프로젝트별 응답 조립
            result.add(ProjectMilestoneResDto.builder()
                    .projectId(project.getId())
                    .projectName(project.getProjectName())
                    .milestoneResDtoList(rootStones)
                    .build());
        }

        return result;
    }





    // 사용자 그룹별 프로젝트 현황 조회
    @Transactional(readOnly = true)
    public List<UserGroupProjectProgressResDto> getUserGroupProjectProgress(String userId, String workspaceId) {

        // 1. 워크스페이스 검증
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스가 존재하지 않습니다."));

        // 개인워크스페이스에는 안되게 방어코드
        validateNotPersonalWorkspace(workspace);

        // 2. 요청자 검증
        WorkspaceParticipant requester = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspaceId, UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 3. 관리자 권한 확인
        if (!requester.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            throw new IllegalArgumentException("관리자만 접근할 수 있습니다.");
        }

        // 4. 해당 워크스페이스의 사용자 그룹 전체 조회
        List<UserGroup> userGroups = userGroupRepository.findByWorkspace(workspace);
        List<UserGroupProjectProgressResDto> result = new ArrayList<>();

        // 5. 각 사용자 그룹별로 프로젝트 진행률 계산
        for (UserGroup group : userGroups) {

            // 5-1. 그룹에 속한 사용자 매핑 조회
            List<UserGroupMapping> mappings = userGroupMappingRepository.findByUserGroup(group);
            if (mappings.isEmpty()) continue;

            // 5-2. 매핑된 워크스페이스 참여자 목록
            List<WorkspaceParticipant> participants = mappings.stream()
                    .map(UserGroupMapping::getWorkspaceParticipant)
                    .toList();

            // 5-3. 해당 참여자들이 속한 프로젝트 참가 정보 조회
            List<ProjectParticipant> projectParticipants =
                    projectParticipantRepository.findAllByWorkspaceParticipantIn(participants);

            // 5-4. 실제 프로젝트 추출 (중복 제거 + 삭제 제외)
            Set<Project> projects = projectParticipants.stream()
                    .map(ProjectParticipant::getProject)
                    .filter(p -> !Boolean.TRUE.equals(p.getIsDelete()))
                    .collect(Collectors.toSet());

            // 5-5. 그룹 내 프로젝트가 없을 경우 0% 처리
            if (projects.isEmpty()) {
                result.add(UserGroupProjectProgressResDto.builder()
                        .groupName(group.getUserGroupName())
                        .projectCount(0)
                        .averageProgress(0)
                        .build());
                continue;
            }

            // 5-6. DB에 저장된 milestone 그대로 사용 (재계산 X)
            double avgProgress = projects.stream()
                    .map(Project::getMilestone)
                    .filter(Objects::nonNull)
                    .mapToDouble(BigDecimal::doubleValue)
                    .average()
                    .orElse(0.0);

            // 5-7. DTO 변환 및 결과 누적
            result.add(UserGroupProjectProgressResDto.builder()
                    .groupName(group.getUserGroupName())
                    .projectCount(projects.size())
                    .averageProgress(Math.round(avgProgress * 10) / 10.0)
                    .build());
        }

        // 6. 최종 결과 반환
        return result;
    }

    // 워크스페이스에 존재하지 않는 회원 목록에서 검색 (이메일 기반)
    public UserInfoListResDto searchParticipants(String userId, SearchDto dto) {

        // 1. 워크스페이스 유효성 검증
        Workspace workspace = workspaceRepository.findById(dto.getWorkspaceId())
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 워크스페이스입니다."));

        // 개인워크스페이스에는 안되게 방어코드
        validateNotPersonalWorkspace(workspace);

        // 2️. 현재 워크스페이스 참가자 목록 추출
        List<UUID> participantUserIds = workspaceParticipantRepository.findByWorkspaceId(workspace.getId())
                .stream()
                .filter(p -> !p.isDelete()) // 삭제된 사람은 제외 (검색 결과에 포함되도록)
                .map(WorkspaceParticipant::getUserId)
                .toList();

        // 3️. user-service에 userIdList 제외한 유저목록 요청
        UserIdListDto excludedIdsDto = UserIdListDto.builder()
                .userIdList(participantUserIds)
                .build();

        UserInfoListResDto userInfoListResDto = userFeign.fetchUsersNotInWorkspace(excludedIdsDto);

        // 4️. 키워드 검색 (optional) — 이메일 기준으로 변경
        if (dto.getSearchKeyword() != null && !dto.getSearchKeyword().isBlank()) {
            List<UserInfoResDto> filtered = userInfoListResDto.getUserInfoList().stream()
                    .filter(user -> user.getUserEmail() != null
                            && user.getUserEmail().contains(dto.getSearchKeyword())) // 추가됨
                    .toList();

            return UserInfoListResDto.builder()
                    .userInfoList(filtered)
                    .build();
        }

        // 5️. 그대로 반환
        return userInfoListResDto;
    }

    // 워크스페이스 내 참여자 검색 (user-service 정보 포함, 이메일 기준)
    @Transactional(readOnly = true)
    public UserInfoListResDto searchWorkspaceParticipants(String userId, SearchDto dto) {

        // 1. 워크스페이스 유효성 검증
        Workspace workspace = workspaceRepository.findById(dto.getWorkspaceId())
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 워크스페이스입니다."));

        // 개인 워크스페이스 방어
        validateNotPersonalWorkspace(workspace);

        // 2. 요청자 권한 검증
        WorkspaceParticipant requester = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(dto.getWorkspaceId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("해당 워크스페이스 접근 권한이 없습니다."));

//        if (!requester.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
//            throw new IllegalArgumentException("관리자만 사용자 조회가 가능합니다.");
//        }

        // 3. 워크스페이스 참여자 목록 조회 (삭제된 사용자 제외)
        List<WorkspaceParticipant> participants =
                workspaceParticipantRepository.findByWorkspaceIdAndIsDeleteFalse(dto.getWorkspaceId());

        if (participants.isEmpty()) {
            return UserInfoListResDto.builder()
                    .userInfoList(Collections.emptyList())
                    .build();
        }

        // 4. 참가자 userId 리스트 생성
        List<UUID> participantUserIds = participants.stream()
                .map(WorkspaceParticipant::getUserId)
                .toList();

        // 5. user-service에서 상세 정보 일괄 조회 (이메일 포함)
        UserInfoListResDto userInfoListResDto = userFeign.fetchUserListInfo(new UserIdListDto(participantUserIds));

        String keyword = dto.getSearchKeyword();

        // 6. 이메일 기준 필터링 및 병합
        List<UserInfoResDto> result = userInfoListResDto.getUserInfoList().stream()
                .filter(info -> info != null && info.getUserId() != null)
                .filter(info -> keyword == null || keyword.isBlank()
                        || (info.getUserEmail() != null && info.getUserEmail().contains(keyword))) // 추가됨
                .map(info -> {
                    // 워크스페이스에 저장된 표시명을 우선 사용(정책 유지)
                    WorkspaceParticipant p = participants.stream()
                            .filter(pp -> pp.getUserId().equals(info.getUserId()))
                            .findFirst()
                            .orElse(null);
                    return UserInfoResDto.builder()
                            .userId(info.getUserId())
                            .userName(p != null ? p.getUserName() : info.getUserName())
                            .userEmail(info.getUserEmail()) // 추가됨
                            .profileImageUrl(info.getProfileImageUrl())
                            .build();
                })
                .toList();

        return UserInfoListResDto.builder()
                .userInfoList(result)
                .build();
    }

    // 워크스페이스 내 사용자 그룹이 없는 참여자 검색(사용자 그룹 추가시 활용, 이메일 기준)
    @Transactional(readOnly = true)
    public UserInfoListResDto searchWorkspaceParticipantsNotInUserGroup(String userId, SearchDto dto) {

        // 1. 워크스페이스 유효성 검증
        Workspace workspace = workspaceRepository.findById(dto.getWorkspaceId())
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 워크스페이스입니다."));

        // 개인 워크스페이스 방어
        validateNotPersonalWorkspace(workspace);

        // 2. 요청자 권한 검증
        WorkspaceParticipant requester = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(dto.getWorkspaceId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("해당 워크스페이스 접근 권한이 없습니다."));

        if (!requester.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            throw new IllegalArgumentException("관리자만 사용자 조회가 가능합니다.");
        }

        // 3. 워크스페이스 참여자 목록 조회 (삭제된 사용자 제외)
        List<WorkspaceParticipant> participants =
                workspaceParticipantRepository.findByWorkspaceIdAndIsDeleteFalse(dto.getWorkspaceId());

        if (participants.isEmpty()) {
            return UserInfoListResDto.builder()
                    .userInfoList(Collections.emptyList())
                    .build();
        }

        // 4. 사용자 그룹에 이미 속한 참여자 제외
        List<WorkspaceParticipant> filteredParticipants = participants.stream()
                .filter(p -> !userGroupMappingRepository.existsByWorkspaceParticipant(p))
                .toList();

        if (filteredParticipants.isEmpty()) {
            return UserInfoListResDto.builder()
                    .userInfoList(Collections.emptyList())
                    .build();
        }

        // 5. 참가자 userId 리스트 생성
        List<UUID> participantUserIds = filteredParticipants.stream()
                .map(WorkspaceParticipant::getUserId)
                .toList();

        // 6. user-service에서 상세 정보 가져오기 (Feign)
        UserInfoListResDto userInfoListResDto =
                userFeign.fetchUserListInfo(UserIdListDto.builder().userIdList(participantUserIds).build());

        // 7. 워크스페이스 참가자와 user-service의 정보 병합
        Map<UUID, UserInfoResDto> userInfoMap = userInfoListResDto.getUserInfoList().stream()
                .collect(Collectors.toMap(UserInfoResDto::getUserId, u -> u));

        // 8. 키워드 필터링 및 병합 — 이메일 기준으로 변경
        String keyword = dto.getSearchKeyword();
        List<UserInfoResDto> filteredList = filteredParticipants.stream()
                .map(p -> {
                    UserInfoResDto info = userInfoMap.get(p.getUserId());
                    return UserInfoResDto.builder()
                            .userId(p.getUserId())
                            .userName(p.getUserName())
                            .userEmail(info != null ? info.getUserEmail() : null)
                            .profileImageUrl(info != null ? info.getProfileImageUrl() : null)
                            .build();
                })
                .filter(u -> keyword == null || keyword.isBlank()
                        || (u.getUserEmail() != null && u.getUserEmail().contains(keyword))) // 추가됨
                .toList();

        // 9. 결과 반환
        return UserInfoListResDto.builder()
                .userInfoList(filteredList)
                .build();
    }


    // 권한그룹이 없는 워크스페이스 참여자 조회
    @Transactional(readOnly = true)
    public List<UserInfoResDto> searchWorkspaceParticipantsNotInAccessGroup(String userId, SearchDto dto) {

        // 1. 워크스페이스 조회
        Workspace workspace = workspaceRepository.findById(dto.getWorkspaceId())
                .orElseThrow(() -> new EntityNotFoundException("해당 워크스페이스가 존재하지 않습니다."));

        // 2. 요청자 검증
        UserInfoResDto requesterInfo = userFeign.fetchUserInfoById(userId);
        WorkspaceParticipant requester = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), requesterInfo.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("요청자는 워크스페이스 참가자가 아닙니다."));

        // 3. 관리자 권한 검증
        if (!requester.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            throw new IllegalArgumentException("관리자 권한이 필요합니다.");
        }

        // 4. 워크스페이스 전체 참여자 목록 조회
        List<WorkspaceParticipant> participants = workspaceParticipantRepository.findByWorkspaceId(workspace.getId());

        // 5. AccessGroup 연결되지 않은 참여자만 필터링
        List<WorkspaceParticipant> notInAccessGroup = participants.stream()
                .filter(p -> p.getAccessGroup() == null)
                .toList();

        // 6. 사용자 정보 조회 (Feign)
        List<UUID> userIds = notInAccessGroup.stream()
                .map(WorkspaceParticipant::getUserId)
                .toList();

        if (userIds.isEmpty()) return List.of();

        UserInfoListResDto userInfoListResDto = userFeign.fetchUserListInfo(new UserIdListDto(userIds));

        // 7. 검색 키워드 필터링
        String keyword = dto.getSearchKeyword();
        return userInfoListResDto.getUserInfoList().stream()
                .filter(u -> keyword == null || keyword.isBlank() || u.getUserName().contains(keyword))
                .toList();
    }

    // 특정 워크스페이스별 완료 안 된 task 목록 조회
    @Transactional(readOnly = true)
    public List<MyTaskResDto> getMyTasksInWorkspace(String userId, String workspaceId) {

        // 1. 현재 유저가 해당 워크스페이스에 속한 참가자 조회
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspaceId, UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("해당 워크스페이스의 참가자가 아닙니다."));

        // 2. 참가자 ID로 담당 중인 Task 조회
        List<Task> tasks = taskRepository.findAllByTaskManagerId(participant.getId())
                .stream()
                .toList();

        // 3. DTO 변환
        return tasks.stream()
                .map(task -> MyTaskResDto.builder()
                        .taskId(task.getId())
                        .taskName(task.getTaskName())
                        .projectName(task.getStone().getProject().getProjectName())
                        .stoneName(task.getStone().getStoneName())
                        .isDone(task.getIsDone())
                        .startTime(task.getStartTime())
                        .endTime(task.getEndTime())
                        .stoneMilestone(task.getStone().getMilestone())
                        .stoneId(task.getStone().getId())
                        .build())
                .toList();
    }

    // 특정 워크스페이스 내 내가 속한 프로젝트 목록 조회
    @Transactional(readOnly = true)
    public List<MyProjectResDto> getMyProjectsInWorkspace(String userId, String workspaceId) {

        // 1. 현재 유저가 워크스페이스에 존재하는지 검증
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspaceId, UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("해당 워크스페이스의 참가자가 아닙니다."));

        // 2. 내가 속한 프로젝트 참가 정보 조회
        List<ProjectParticipant> projectParticipants =
                projectParticipantRepository.findAllByWorkspaceParticipant(participant);

        // 3. 프로젝트 중복 제거 및 삭제 제외
        Set<Project> projects = projectParticipants.stream()
                .map(ProjectParticipant::getProject)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDelete()))
                .collect(Collectors.toSet());

        // 4. DTO 변환
        return projects.stream()
                .map(project -> MyProjectResDto.builder()
                        .projectId(project.getId())
                        .projectName(project.getProjectName())
                        .startTime(project.getStartTime())
                        .endTime(project.getEndTime())
                        .milestone(project.getMilestone() != null ? project.getMilestone() : BigDecimal.ZERO)
                        .build())
                .toList();
    }





    public void validateNotPersonalWorkspace(Workspace workspace) {
        if (workspace.getWorkspaceTemplates() == WorkspaceTemplates.PERSONAL) {
            throw new IllegalArgumentException("개인 워크스페이스에서는 이 작업을 수행할 수 없습니다.");
        }
    }

    // 워크스페이스 담당자 확인
    public boolean checkWorkspaceManager(String workspaceId, String userId) {
        UUID uuidUserId = UUID.fromString(userId);
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspaceId, uuidUserId)
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자 정보를 찾을 수 없습니다."));

        return participant.getWorkspaceRole() == WorkspaceRole.ADMIN;
    }

    // 워크스페이스 참여자인지 리턴
    public boolean checkWorkspaceMembership(String workspaceId, String userId) {
        UUID uuidUserId = UUID.fromString(userId);
        return workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspaceId, uuidUserId)
                .isPresent();
    }

    // 프로젝트 담당자 확인
    public boolean checkProjectManagership(String projectId, String userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다."));
        UUID uuid = UUID.fromString(userId);
        return project.getWorkspaceParticipant().getUserId().equals(uuid);
    }

    // 프로젝트 참여자 확인
    public boolean checkProjectMembership(String projectId, String userId) {
        UUID uuid = UUID.fromString(userId);
        return projectParticipantRepository
                .findByProjectIdAndUserId(projectId, uuid)
                .isPresent();
    }

    // 스톤 담당자 확인
    public boolean checkStoneManagership(String stoneId, String userId) {
        Stone stone = stoneRepository.findById(stoneId)
                .orElseThrow(() -> new EntityNotFoundException("스톤을 찾을 수 없습니다."));
        UUID uuid = UUID.fromString(userId);
        return stone.getStoneManager().getUserId().equals(uuid);
    }

    // 스톤 참여자 확인
    public boolean checkStoneMembership(String stoneId, String userId) {
        UUID uuid = UUID.fromString(userId);
        return stoneParticipantRepository
                .findByStoneIdAndUserId(stoneId, uuid)
                .isPresent();
    }

    // workspaceId 넘겼을 때 하위 프로젝트 Id, 프로젝트명 가져오는 api
    public List<SubProjectResDto> getSubProjectsByWorkspace(String workspaceId) {
        return projectRepository.findAllByWorkspaceId(workspaceId).stream()
                .filter(project -> !project.getIsDelete())
                .map(project -> SubProjectResDto.builder()
                        .projectId(project.getId())
                        .projectName(project.getProjectName())
                        .build())
                .toList();
    }

    //projectId 넘겼을 때 하위 스톤 id, 테스크명 가져오는 api
    public StoneTaskResDto getSubStonesAndTasks(String projectId) {

        // 하위 스톤
        List<StoneTaskResDto.StoneInfo> stones = stoneRepository.findAllByProjectId(projectId).stream()
                .filter(stone -> !stone.getIsDelete()) // 삭제된거 제외
                .map(stone -> StoneTaskResDto.StoneInfo.builder() // 스톤객체에서 아이디랑 이름만 가져옴
                        .stoneId(stone.getId())
                        .stoneName(stone.getStoneName())
                        .build())
                .toList();

        // 하위 태스크
        List<StoneTaskResDto.TaskInfo> tasks = taskRepository.findAllByProjectId(projectId).stream()
                .map(task -> StoneTaskResDto.TaskInfo.builder() // 태스크에서 아이디 이름 가져오옴, 태스크는 하드삭제입니다
                        .taskId(task.getId())
                        .taskName(task.getTaskName())
                        .build())
                .toList();

        return StoneTaskResDto.builder()
                .stones(stones)
                .tasks(tasks)
                .build();
    }

    // WorkspaceService.java

    @Transactional(readOnly = true)
    public WorkspaceOrProjectManagerCheckDto checkWorkspaceOrProjectManager(String stoneId, String userId) {

        // 1️. 스톤 조회
        Stone stone = stoneRepository.findById(stoneId)
                .orElseThrow(() -> new EntityNotFoundException("스톤을 찾을 수 없습니다."));

        // 2️. 프로젝트, 워크스페이스 추적
        Project project = stone.getProject();
        Workspace workspace = project.getWorkspace();

        // 3️. 요청자 정보 조회
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("해당 워크스페이스의 참가자가 아닙니다."));

        // 4️. 관리자 여부 판단
        boolean isWorkspaceManager = participant.getWorkspaceRole().equals(WorkspaceRole.ADMIN);
        boolean isProjectManager = project.getWorkspaceParticipant().equals(participant);

        // 5️. DTO 반환
        return WorkspaceOrProjectManagerCheckDto.builder()
                .isWorkspaceManager(isWorkspaceManager)
                .isProjectManager(isProjectManager)
                .build();
    }


    // 워크스페이스id, 프젝id, 스톤id 중 하나 넘겼을 때 해당 이름 받아오는 api
    public EntityNameResDto getEntityName(EntityNameReqDto dto) {

        if (dto.getWorkspaceId() != null) {
            return workspaceRepository.findById(dto.getWorkspaceId())
                    .filter(ws -> !ws.getIsDelete())
                    .map(ws -> EntityNameResDto.builder()
                            .type("workspace")
                            .id(ws.getId())
                            .name(ws.getWorkspaceName())
                            .build())
                    .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 워크스페이스입니다."));
        }

        if (dto.getProjectId() != null) {
            return projectRepository.findById(dto.getProjectId())
                    .filter(p -> !p.getIsDelete())
                    .map(p -> EntityNameResDto.builder()
                            .type("project")
                            .id(p.getId())
                            .name(p.getProjectName())
                            .build())
                    .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 프로젝트입니다."));
        }

        if (dto.getStoneId() != null) {
            return stoneRepository.findById(dto.getStoneId())
                    .filter(s -> !s.getIsDelete())
                    .map(s -> EntityNameResDto.builder()
                            .type("stone")
                            .id(s.getId())
                            .name(s.getStoneName())
                            .build())
                    .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 스톤입니다."));
        }

        throw new IllegalArgumentException("workspaceId, projectId, stoneId 중 하나는 반드시 필요합니다.");
    }

    // 워크스페이스 내 나의 스톤 목록 조회 (루트스톤 제외)
    @Transactional(readOnly = true)
    public List<MyStoneResDto> getMyStonesInWorkspace(String userId, String workspaceId) {

        // 1. 워크스페이스 참가자 검증
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspaceId, UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("해당 워크스페이스 참가자가 아닙니다."));

        // 2. 내가 참여 중인 스톤 전부 조회 (루트 스톤 제외, isDelete=false)
        List<StoneParticipant> myStoneParticipants =
                stoneParticipantRepository.findAllActiveWithStoneByWorkspaceParticipant(participant);

        // 3. 루트 스톤 제외 (parentStoneId != null)
        List<Stone> filteredStones = myStoneParticipants.stream()
                .map(StoneParticipant::getStone)
                .filter(stone -> stone.getParentStoneId() != null) // 루트 스톤 제외
                .toList();

        // 4. DTO 변환
        return filteredStones.stream()
                .map(stone -> MyStoneResDto.builder()
                        .stoneId(stone.getId())
                        .stoneName(stone.getStoneName())
                        .projectName(stone.getProject().getProjectName())
                        .milestone(stone.getMilestone() != null ? stone.getMilestone() : BigDecimal.ZERO)
                        .startTime(stone.getStartTime())
                        .endTime(stone.getEndTime())
                        .projectId(stone.getProject().getId())
                        .build())
                .toList();
    }

    // 접근 권한 유저ID 리턴
    public Set<String> getViewableUserIds(String rootId, String rootType){
        Set<String> viewableUserIds = new HashSet<>();
        // 워크스페이스일 경우
        if(rootType.equals("WORKSPACE")){
            Workspace workspace = workspaceRepository.findById(rootId).orElseThrow(()->new EntityNotFoundException("존재하지 않는 워크스페이스입니다."));
            List<WorkspaceParticipant> workspaceParticipants = workspaceParticipantRepository.findAllByWorkspace(workspace);
            for(WorkspaceParticipant workspaceParticipant : workspaceParticipants){
                viewableUserIds.add(workspaceParticipant.getUserId().toString());
            }
        }else if(rootType.equals("PROJECT")){
            Project project = projectRepository.findById(rootId).orElseThrow(()->new EntityNotFoundException("존재하지 않는 프로젝트입니다."));
            List<ProjectParticipant> projectParticipants = projectParticipantRepository.findAllByProject(project);
            for(ProjectParticipant projectParticipant : projectParticipants){
                viewableUserIds.add(projectParticipant.getWorkspaceParticipant().getUserId().toString());
            }
            viewableUserIds.add(workspaceParticipantRepository.findByWorkspaceIdAndWorkspaceRole(project.getWorkspace().getId(), WorkspaceRole.ADMIN).getUserId().toString());
        }else if(rootType.equals("STONE")){
            Stone stone = stoneRepository.findById(rootId).orElseThrow(()->new EntityNotFoundException("존재하지 않는 스톤입니다."));
            List<StoneParticipant> stoneParticipants = stoneParticipantRepository.findAllByStone(stone);
            for(StoneParticipant stoneParticipant : stoneParticipants){
                viewableUserIds.add(stoneParticipant.getWorkspaceParticipant().getUserId().toString());
            }
            viewableUserIds.add(workspaceParticipantRepository.findByWorkspaceIdAndWorkspaceRole(stone.getProject().getWorkspace().getId(), WorkspaceRole.ADMIN).getUserId().toString());
            viewableUserIds.add((stone.getProject().getWorkspaceParticipant().getUserId().toString()));
        }
        return viewableUserIds;
    }





}
