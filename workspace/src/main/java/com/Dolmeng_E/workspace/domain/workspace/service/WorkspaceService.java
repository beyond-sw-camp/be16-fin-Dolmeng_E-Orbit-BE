package com.Dolmeng_E.workspace.domain.workspace.service;

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
import com.Dolmeng_E.workspace.domain.stone.dto.MilestoneResDto;
import com.Dolmeng_E.workspace.domain.stone.dto.ProjectMilestoneResDto;
import com.Dolmeng_E.workspace.domain.stone.entity.ChildStoneList;
import com.Dolmeng_E.workspace.domain.stone.entity.Stone;
import com.Dolmeng_E.workspace.domain.user_group.dto.UserGroupAddUserDto;
import com.Dolmeng_E.workspace.domain.user_group.entity.UserGroup;
import com.Dolmeng_E.workspace.domain.user_group.entity.UserGroupMapping;
import com.Dolmeng_E.workspace.domain.user_group.repository.UserGroupMappingRepository;
import com.Dolmeng_E.workspace.domain.user_group.repository.UserGroupRepository;
import com.Dolmeng_E.workspace.domain.user_group.service.UserGroupService;
import com.Dolmeng_E.workspace.domain.workspace.dto.*;
import com.Dolmeng_E.workspace.domain.workspace.entity.Workspace;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceInvite;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceRole;
import com.Dolmeng_E.workspace.domain.workspace.repository.WorkspaceInviteRepository;
import com.Dolmeng_E.workspace.domain.workspace.repository.WorkspaceParticipantRepository;
import com.Dolmeng_E.workspace.domain.workspace.repository.WorkspaceRepository;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
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
public class WorkspaceService {
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceParticipantRepository workspaceParticipantRepository;
    private final UserFeign userFeign;
    private final AccessGroupService accessGroupService;
    private final AccessGroupRepository accessGroupRepository;
    private final EmailService emailService;
    private final WorkspaceInviteRepository workspaceInviteRepository;
    private final ProjectParticipantRepository projectParticipantRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserGroupMappingRepository userGroupMappingRepository;

//    워크스페이스 생성
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

//    회원가입 시 워크스페이스 생성

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

    //    워크스페이스 변경(To-do: 관리자 그룹, 일반사용자 그룹은 이름 바꾸지 못하게)
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

    //    워크스페이스 회원 초대
    public void addParticipants(String userId, String workspaceId, WorkspaceAddUserDto dto) {

        // 1. 워크스페이스 확인
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스를 찾을 수 없습니다."));

        // 2. 요청자 권한 확인
        UserInfoResDto requester = userFeign.fetchUserInfoById(userId);
        WorkspaceParticipant admin = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspaceId, requester.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("요청자 워크스페이스 참가자 정보가 없습니다."));

        if (!admin.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            throw new IllegalArgumentException("관리자만 사용자 추가 가능");
        }

        // 3. 유저 리스트
        UserIdListDto userIdListDto = new UserIdListDto(dto.getUserIdList());
        UserInfoListResDto userInfoListResDto = userFeign.fetchUserListInfo(userIdListDto);

        // 관리자는 워크스페이스 초대 안되게끔 제외 리스트 생성
        List<UserInfoResDto> filteredUserList = userInfoListResDto.getUserInfoList().stream()
                .filter(userInfo -> !userInfo.getUserId().equals(requester.getUserId()))
                .toList();

        if (filteredUserList.isEmpty()) {
            throw new IllegalArgumentException("초대 가능한 사용자가 없습니다. (관리자 본인은 제외됩니다)");
        }

        // 4. 신규 사용자 추가 (이름 매핑)
        AccessGroup commonAccessGroup = accessGroupRepository.findByWorkspaceIdAndAccessGroupName(workspaceId, "일반 유저 그룹")
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 ID 혹은 권한 그룹명에 맞는 정보가 없습니다."));

        List<WorkspaceParticipant> newParticipants = new ArrayList<>();

        // 로직추가: 이미 삭제(isDelete=true)된 사용자는 복귀 처리
        for (UserInfoResDto userInfo : filteredUserList) {
            Optional<WorkspaceParticipant> existing = workspaceParticipantRepository
                    .findByWorkspaceIdAndUserId(workspaceId, userInfo.getUserId());

            if (existing.isPresent()) {
                WorkspaceParticipant participant = existing.get();
                if (participant.isDelete()) {
                    participant.restoreParticipant();
                } else {
                    // 이미 활성화된 유저라면 건너뜀
                    continue;
                }
            } else {
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
        }

        try {
            workspaceParticipantRepository.saveAll(newParticipants);
            workspaceParticipantRepository.flush(); // 즉시 insert 실행
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("이미 워크스페이스에 존재하는 사용자가 포함되어 있습니다.");
        }
    }



//    워크스페이스 이메일 회원 초대 (ToDo : 로직 반드시 수정 할 것, X-User-Id 로 바뀌어서 그에 맞게!)
    public void inviteUsers(String userId, String workspaceId, WorkspaceInviteDto dto) throws AccessDeniedException {
        // 1. 워크스페이스 확인
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스를 찾을 수 없습니다."));

        // 2. 요청자 권한 확인
        UserInfoResDto requester = userFeign.fetchUserInfoById(userId);
        WorkspaceParticipant admin = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspaceId, requester.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("요청자 워크스페이스 참가자 정보가 없습니다."));

        if(admin.getWorkspaceRole()!=WorkspaceRole.ADMIN) {
            throw new AccessDeniedException("관리자 권한이 필요합니다.");
        }

        for (String email : dto.getInviteEmails()) {

            // 1. User 서비스에 회원 존재 여부 확인
            boolean userExists;
            try {
                userFeign.fetchUserInfoById(email);
                userExists = true;
            } catch (FeignException.NotFound e) {
                userExists = false;
            }

            // 초대코드 생성
            String inviteCode = UUID.randomUUID().toString();

            // 2. 초대 정보 저장 (회원 여부 포함)
            WorkspaceInvite invite = WorkspaceInvite.builder()
                    .workspace(workspace)
                    .inviteEmail(email)
                    .inviteCode(inviteCode)
                    .isAccepted(false)
                    .isExistingUser(userExists)   // 분기 기준 저장
                    .expiresAt(LocalDateTime.now().plusDays(3))
                    .build();

            workspaceInviteRepository.save(invite);

            // 3. 이메일 발송
            emailService.sendInviteMail(email, inviteCode, workspace.getWorkspaceName(), userExists);
        }
}

//    워크스페이스 참여자 목록 조회
    @Transactional(readOnly = true)
    public Page<WorkspaceParticipantResDto> getWorkspaceParticipants(String userId, String workspaceId) {
        // 1. 요청자 유효성 검증
        UserInfoResDto requester = userFeign.fetchUserInfoById(userId);

        // 2. 워크스페이스 조회
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("해당 워크스페이스를 찾을 수 없습니다."));

        // 해당 워크스페이스의 관리자만 가능하도록 방어 코드
        WorkspaceParticipant admin = workspaceParticipantRepository.findByWorkspaceIdAndUserId(workspaceId, UUID.fromString(userId))
                .orElseThrow(()->new EntityNotFoundException("참여자 정보가 없습니다."));
        if(!admin.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            throw new IllegalArgumentException(("해당 워크스페이스의 관리자만 조회 가능합니다."));
        }

        // 3. 내부에서 페이지 및 정렬 설정 (createdAt ASC, 8개씩)
        Pageable pageable = PageRequest.of(0, 8, Sort.by("createdAt").ascending());

        // 4. 참여자 조회 (삭제된 사람 포함)
        Page<WorkspaceParticipant> participantPage =
                workspaceParticipantRepository.findAllByWorkspaceId(workspaceId, pageable);

        // 5. DTO 변환
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
    public void deleteWorkspaceParticipants(String userId, String workspaceId, WorkspaceDeleteUserDto dto) {
        // 1. 관리자 확인
        UserInfoResDto adminInfo = userFeign.fetchUserInfoById(userId);

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("해당 워크스페이스를 찾을 수 없습니다."));

        WorkspaceParticipant admin = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspaceId, adminInfo.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("요청자는 워크스페이스에 존재하지 않습니다."));

        if (!admin.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            throw new EntityNotFoundException("관리자만 회원 삭제가 가능합니다.");
        }

        // 2. 자기 자신 포함 방지
        if (dto.getUserIdList().contains(adminInfo.getUserId())) {
            throw new IllegalArgumentException("관리자는 자기 자신을 삭제할 수 없습니다.");
        }

        // 3. 유저 리스트 순회하면서 논리 삭제 처리
        for (UUID targetUserId : dto.getUserIdList()) {
            WorkspaceParticipant target = workspaceParticipantRepository
                    .findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                    .orElseThrow(() -> new EntityNotFoundException("삭제 대상 사용자를 찾을 수 없습니다."));

            if (!target.isDelete()) {
                target.deleteParticipant();
                workspaceParticipantRepository.save(target);
            }
        }
    }

    // 워크스페이스 삭제
    public void deleteWorkspace(String userId, String workspaceId) {
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

        // 5. 삭ㅂ제
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
                    project.updateMilestone(); // 완료/전체 비율 재계산
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

    // 워크스페이스에 존재하지 않는 회원 목록에서 검색
    public UserInfoListResDto searchParticipants(String userId, SearchDto dto) {

        // 1. 워크스페이스 유효성 검증
        Workspace workspace = workspaceRepository.findById(dto.getWorkspaceId())
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 워크스페이스입니다."));

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

        // 4️. 키워드 검색 (optional)
        if (dto.getSearchKeyword() != null && !dto.getSearchKeyword().isBlank()) {
            List<UserInfoResDto> filtered = userInfoListResDto.getUserInfoList().stream()
                    .filter(user -> user.getUserName().contains(dto.getSearchKeyword()))
                    .toList();

            return UserInfoListResDto.builder()
                    .userInfoList(filtered)
                    .build();
        }

        // 5️. 그대로 반환
        return userInfoListResDto;
    }

    // 워크스페이스 내 참여자 검색
    @Transactional(readOnly = true)
    public UserInfoListResDto searchWorkspaceParticipants(String userId, SearchDto dto) {

        // 1. 워크스페이스 유효성 검증
        Workspace workspace = workspaceRepository.findById(dto.getWorkspaceId())
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 워크스페이스입니다."));

        // 2. 요청자 유효성 검증
        WorkspaceParticipant requester = workspaceParticipantRepository.findByWorkspaceIdAndUserId(dto.getWorkspaceId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("해당 워크스페이스 접근 권한이 없습니다."));

        if (!requester.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            throw new IllegalArgumentException("관리자만 사용자 추가 가능");
        }

        // 3. 워크스페이스 참여자 목록 조회 (삭제 안된 사용자만)
        List<WorkspaceParticipant> participants =
                workspaceParticipantRepository.findByWorkspaceIdAndIsDeleteFalse(dto.getWorkspaceId());

        // 4. 키워드 필터링 (없으면 전체)
        String keyword = dto.getSearchKeyword();
        List<WorkspaceParticipant> filteredParticipants = (keyword == null || keyword.isBlank())
                ? participants
                : participants.stream()
                .filter(p -> p.getUserName().contains(keyword))
                .toList();

        // 5. DTO 변환
        List<UserInfoResDto> userInfoList = filteredParticipants.stream()
                .map(p -> UserInfoResDto.builder()
                        .userId(p.getUserId())
                        .userName(p.getUserName())
                        .build())
                .toList();

        // 6. 반환
        return UserInfoListResDto.builder().userInfoList(userInfoList).build();
    }


}
