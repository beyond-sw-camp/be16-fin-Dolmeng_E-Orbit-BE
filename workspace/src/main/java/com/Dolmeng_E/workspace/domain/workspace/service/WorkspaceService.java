package com.Dolmeng_E.workspace.domain.workspace.service;

import com.Dolmeng_E.workspace.common.dto.UserIdListDto;
import com.Dolmeng_E.workspace.common.dto.UserInfoListResDto;
import com.Dolmeng_E.workspace.common.dto.UserInfoResDto;
import com.Dolmeng_E.workspace.common.service.UserFeign;
import com.Dolmeng_E.workspace.domain.access_group.entity.AccessGroup;
import com.Dolmeng_E.workspace.domain.access_group.repository.AccessGroupRepository;
import com.Dolmeng_E.workspace.domain.access_group.service.AccessGroupService;
import com.Dolmeng_E.workspace.domain.workspace.dto.WorkspaceAddUserDto;
import com.Dolmeng_E.workspace.domain.workspace.dto.WorkspaceCreateDto;
import com.Dolmeng_E.workspace.domain.workspace.dto.WorkspaceInviteDto;
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
    public String createWorkspace(WorkspaceCreateDto workspaceCreateDto, String userEmail) {


        // 1. 워크스페이스 생성
        UserInfoResDto userInfoResDto = userFeign.fetchUserInfo(userEmail);
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

//    워크스페이스 수정

//    워크스페이스 변경

//    워크스페이스 회원 초대
public void addParticipants(String userEmail, String workspaceId, WorkspaceAddUserDto dto) {

    // 1. 워크스페이스 확인
    Workspace workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new EntityNotFoundException("워크스페이스를 찾을 수 없습니다."));

    // 2. 요청자 권한 확인
    UserInfoResDto requester = userFeign.fetchUserInfo(userEmail);
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


//    워크스페이스 이메일 회원 초대
    public void inviteUsers(String adminEmail, String workspaceId, WorkspaceInviteDto dto) throws AccessDeniedException {
        // 1. 워크스페이스 확인
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스를 찾을 수 없습니다."));

        // 2. 요청자 권한 확인
        UserInfoResDto requester = userFeign.fetchUserInfo(adminEmail);
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
                userFeign.fetchUserInfo(email);
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

//    워크스페이스 회원 삭제

}
