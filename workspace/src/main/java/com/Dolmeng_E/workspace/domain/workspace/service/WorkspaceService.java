package com.Dolmeng_E.workspace.domain.workspace.service;

import com.Dolmeng_E.workspace.domain.workspace.dto.UserInfoResDto;
import com.Dolmeng_E.workspace.domain.workspace.dto.WorkspaceCreateDto;
import com.Dolmeng_E.workspace.domain.workspace.entity.Workspace;
import com.Dolmeng_E.workspace.domain.workspace.repository.WorkspaceParticipantRepository;
import com.Dolmeng_E.workspace.domain.workspace.repository.WorkspaceRepository;
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

//    워크스페이스 생성
    public String createWorkspace(WorkspaceCreateDto workspaceCreateDto, String userEmail) {

        UserInfoResDto userInfoResDto = userFeign.fetchUserInfo(userEmail);
        Workspace workspace = workspaceCreateDto.toEntity(userInfoResDto.getUserId());
        workspace.settingMaxStorage(workspaceCreateDto.getWorkspaceTemplates());
        workspaceRepository.save(workspace);

        return workspace.getId();
    }

//    회원가입 시 워크스페이스 생성

//    워크스페이스 목록 조회

//    워크스페이스 수정

//    워크스페이스 변경

}
