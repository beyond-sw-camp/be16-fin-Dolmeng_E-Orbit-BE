package com.Dolmeng_E.workspace.domain.workspace.service;

import com.Dolmeng_E.workspace.common.dto.UserInfoResDto;
import com.Dolmeng_E.workspace.common.service.UserFeign;
import com.Dolmeng_E.workspace.domain.access_group.entity.AccessGroup;
import com.Dolmeng_E.workspace.domain.access_group.repository.AccessGroupRepository;
import com.Dolmeng_E.workspace.domain.access_group.service.AccessGroupService;
import com.Dolmeng_E.workspace.domain.workspace.dto.WorkspaceCreateDto;
import com.Dolmeng_E.workspace.domain.workspace.entity.Workspace;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceRole;
import com.Dolmeng_E.workspace.domain.workspace.repository.WorkspaceParticipantRepository;
import com.Dolmeng_E.workspace.domain.workspace.repository.WorkspaceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class WorkspaceService {
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceParticipantRepository workspaceParticipantRepository;
    private final UserFeign userFeign;
    private final AccessGroupService accessGroupService;
    private final AccessGroupRepository accessGroupRepository;

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

//    워크스페이스 가입

}
