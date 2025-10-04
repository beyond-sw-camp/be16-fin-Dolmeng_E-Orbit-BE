package com.Dolmeng_E.workspace.domain.access_group.service;

import com.Dolmeng_E.workspace.domain.access_group.entity.AccessDetail;
import com.Dolmeng_E.workspace.domain.access_group.entity.AccessGroup;
import com.Dolmeng_E.workspace.domain.access_group.entity.AccessList;
import com.Dolmeng_E.workspace.domain.access_group.entity.AccessType;
import com.Dolmeng_E.workspace.domain.access_group.repository.AccessDetailRepository;
import com.Dolmeng_E.workspace.domain.access_group.repository.AccessGroupRepository;
import com.Dolmeng_E.workspace.domain.access_group.repository.AccessListRepository;
import com.Dolmeng_E.workspace.domain.workspace.entity.Workspace;
import com.Dolmeng_E.workspace.domain.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class AccessGroupService {

    private final AccessGroupRepository accessGroupRepository;
    private final AccessDetailRepository accessDetailRepository;
    private final AccessListRepository accessListRepository;
    private final WorkspaceRepository workspaceRepository;

    // 관리자 권한 그룹 생성 (워크스페이스 ID 기반, 워크스페이스 생성시 자동생성)
    public void createAdminGroupForWorkspace(String workspaceId) {

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없습니다. ID=" + workspaceId));

        AccessGroup adminGroup = AccessGroup.builder()
                .workspace(workspace)
                .accessGroupName("관리자 그룹")
                .build();
        accessGroupRepository.save(adminGroup);

        for (AccessType type : AccessType.values()) {
            AccessList accessList = accessListRepository.findByAccessType(type)
                    .orElseThrow(() -> new IllegalStateException("AccessList 정의 없음: " + type));

            AccessDetail detail = AccessDetail.builder()
                    .accessGroup(adminGroup)
                    .accessList(accessList)
                    .isAccess(true)
                    .build();

            accessDetailRepository.save(detail);
        }
    }


    //    일반유저 권한그룹 생성(워크스페이스 생성시 자동생성)
    public void createDefaultUserGroup(String workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없습니다. ID=" + workspaceId));

        AccessGroup defaultUserGroup = AccessGroup.builder()
                .workspace(workspace)
                .accessGroupName("일반 유저 그룹")
                .build();
        accessGroupRepository.save(defaultUserGroup);

        // 스톤의 파일 조회만 허용(임시)
        Set<AccessType> defaultTruePermissions = Set.of(AccessType.STONE_FILE_VIEW);

        for (AccessType type : AccessType.values()) {
            AccessList accessList = accessListRepository.findByAccessType(type)
                    .orElseThrow(() -> new IllegalStateException("AccessList 정의 없음: " + type));

            boolean isAccess = defaultTruePermissions.contains(type);

            AccessDetail detail = AccessDetail.builder()
                    .accessGroup(defaultUserGroup)
                    .accessList(accessList)
                    .isAccess(isAccess)
                    .build();

            accessDetailRepository.save(detail);
        }


    }

    //    커스터마이징 권한그룹 생성

    //    권한그룹 수정

    //    권한그룹 리스트 조회

    //    권한그룹 상세 조회

    //    권한그룹 사용자 추가

    //    권한그룹 사용자 수정

    //    권한그룹 사용자 제거

    //    권한그룹 삭제
}
