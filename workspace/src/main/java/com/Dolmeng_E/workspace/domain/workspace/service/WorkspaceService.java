package com.Dolmeng_E.workspace.domain.workspace.service;

import com.Dolmeng_E.workspace.common.dto.UserIdListDto;
import com.Dolmeng_E.workspace.common.dto.UserInfoListResDto;
import com.Dolmeng_E.workspace.common.dto.UserInfoResDto;
import com.Dolmeng_E.workspace.common.service.UserFeign;
import com.Dolmeng_E.workspace.domain.access_group.entity.AccessGroup;
import com.Dolmeng_E.workspace.domain.access_group.repository.AccessGroupRepository;
import com.Dolmeng_E.workspace.domain.access_group.service.AccessGroupService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

        // 3. DTO 변환
        return participants.stream()
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

        // 4. 신규 사용자 추가 (이름 매핑)
        AccessGroup commonAccessGroup = accessGroupRepository.findByWorkspaceIdAndAccessGroupName(workspaceId,"일반 유저 그룹")
                .orElseThrow(()->new EntityNotFoundException("워크스페이스 ID 혹은 권한 그룹명에 맞는 정보가 없습니다."));
        List<WorkspaceParticipant> newParticipants = userInfoListResDto.getUserInfoList().stream()
                .map(userInfo -> WorkspaceParticipant.builder()
                        .workspace(workspace)
                        .workspaceRole(WorkspaceRole.COMMON)
                        .userId(userInfo.getUserId())
                        .accessGroup(commonAccessGroup)
                        .userName(userInfo.getUserName())
                        .isDelete(false)
                        .build())
                .toList();

            workspaceParticipantRepository.saveAll(newParticipants);
    }


//    워크스페이스 이메일 회원 초대 (To-Do : 로직 반드시 수정 할 것, X-User-Id 로 바뀌어서 그에 맞게!)
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

//    워크스페이스 회원 목록 조회
    @Transactional(readOnly = true)
    public List<WorkspaceParticipantResDto> getWorkspaceParticipants(String userId, String workspaceId) {
        // 1. 요청자 유효성 검증
        UserInfoResDto requester = userFeign.fetchUserInfoById(userId);

        // 2. 워크스페이스 조회
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("해당 워크스페이스를 찾을 수 없습니다."));

        // 4. 전체 참여자 조회
        List<WorkspaceParticipant> participants = workspaceParticipantRepository.findAllByWorkspaceId(workspaceId);

        // 5. DTO 변환 (삭제 상태 처리)
        return participants.stream()
                .map(p -> WorkspaceParticipantResDto.builder()
                        .userId(p.getUserId())
                        .userName(p.getUserName())
                        .workspaceRole(p.getWorkspaceRole().name())
                        .isDeleted(p.isDelete())
                        .accessGroupId(p.getAccessGroup() != null ? p.getAccessGroup().getId() : null)
                        .accessGroupName(p.getAccessGroup() != null ? p.getAccessGroup().getAccessGroupName() : null)
                        .build())
                .toList();
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
                target.setDelete(true);
                workspaceParticipantRepository.save(target);
            }
        }
    }

}
