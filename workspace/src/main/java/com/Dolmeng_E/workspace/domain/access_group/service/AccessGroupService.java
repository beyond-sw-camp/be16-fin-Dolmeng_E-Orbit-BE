package com.Dolmeng_E.workspace.domain.access_group.service;

import com.Dolmeng_E.workspace.common.dto.UserInfoResDto;
import com.Dolmeng_E.workspace.common.service.UserFeign;
import com.Dolmeng_E.workspace.domain.access_group.dto.AccessGroupListResDto;
import com.Dolmeng_E.workspace.domain.access_group.dto.AccessGroupModifyDto;
import com.Dolmeng_E.workspace.domain.access_group.dto.CustomAccessGroupDto;
import com.Dolmeng_E.workspace.domain.access_group.entity.AccessDetail;
import com.Dolmeng_E.workspace.domain.access_group.entity.AccessGroup;
import com.Dolmeng_E.workspace.domain.access_group.entity.AccessList;
import com.Dolmeng_E.workspace.domain.access_group.entity.AccessType;
import com.Dolmeng_E.workspace.domain.access_group.repository.AccessDetailRepository;
import com.Dolmeng_E.workspace.domain.access_group.repository.AccessGroupRepository;
import com.Dolmeng_E.workspace.domain.access_group.repository.AccessListRepository;
import com.Dolmeng_E.workspace.domain.workspace.entity.Workspace;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceRole;
import com.Dolmeng_E.workspace.domain.workspace.repository.WorkspaceParticipantRepository;
import com.Dolmeng_E.workspace.domain.workspace.repository.WorkspaceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class AccessGroupService {

    private final AccessGroupRepository accessGroupRepository;
    private final AccessDetailRepository accessDetailRepository;
    private final AccessListRepository accessListRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserFeign userFeign;
    private final WorkspaceParticipantRepository workspaceParticipantRepository;


//    To-Do : 모든 로직 구현 후 try-catch 작업 해야함

    // 관리자 권한 그룹 생성 (워크스페이스 ID 기반, 워크스페이스 생성시 자동생성)
    public String createAdminGroupForWorkspace(String workspaceId) {

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
        return adminGroup.getId();
    }


    //    일반유저 권한그룹 생성(워크스페이스 생성시 자동생성)
    public String createDefaultUserAccessGroup(String workspaceId) {
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
        return defaultUserGroup.getId();


    }

    //    커스터마이징 권한그룹 생성

    public void createCustomAccessGroup(CustomAccessGroupDto dto) {

        // 1. 워크스페이스 조회
        Workspace workspace = workspaceRepository.findById(dto.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없습니다. ID=" + dto.getWorkspaceId()));

        // 2. AccessGroup 생성
        AccessGroup newGroup = AccessGroup.builder()
                .workspace(workspace)
                .accessGroupName(dto.getAccessGroupName())
                .build();

        accessGroupRepository.save(newGroup);

        // 3️. AccessType 순서대로 accessList 값 매핑
        AccessType[] types = AccessType.values();
        List<Boolean> accessValues = dto.getAccessList();

        for (int i = 0; i < types.length; i++) {
            AccessType type = types[i];
            boolean isAccess = i < accessValues.size() && Boolean.TRUE.equals(accessValues.get(i));

            // AccessList (권한 정의 테이블) 조회
            AccessList accessList = accessListRepository.findByAccessType(type)
                    .orElseThrow(() -> new IllegalStateException("AccessList 정의 없음: " + type));

            // AccessDetail 생성 및 저장
            AccessDetail detail = AccessDetail.builder()
                    .accessGroup(newGroup)
                    .accessList(accessList)
                    .isAccess(isAccess)
                    .build();

            accessDetailRepository.save(detail);
        }
    }

    //    권한그룹 수정

    public void modifyAccessGroup(AccessGroupModifyDto dto, String userEmail) {
        // 1. 워크스페이스 조회
        Workspace workspace = workspaceRepository.findById(dto.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없습니다. ID=" + dto.getWorkspaceId()));
        // 2. 유저 권한 체크
        UserInfoResDto userInfoResDto = userFeign.fetchUserInfo(userEmail);
        WorkspaceParticipant workspaceParticipant = workspaceParticipantRepository.findByWorkspaceIdAndUserId(workspace.getId(),userInfoResDto.getUserId())
                .orElseThrow(()->new EntityNotFoundException("워크스페이스 참여자를 찾을 수 없습니다."));
        if (workspaceParticipant.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            // 1. 수정할 AccessGroup 조회
            AccessGroup targetGroup = accessGroupRepository.findByWorkspaceIdAndAccessGroupName(
                    workspace.getId(),
                    dto.getAccessGroupName()
            ).orElseThrow(() -> new EntityNotFoundException("수정 대상 권한 그룹을 찾을 수 없습니다."));

            // 2. AccessType 목록 가져오기
            AccessType[] types = AccessType.values();
            List<Boolean> accessValues = dto.getAccessList();

            // 3. 기존 AccessDetail 목록 조회
            List<AccessDetail> existingDetails = accessDetailRepository.findByAccessGroup(targetGroup);

            // 4. 권한그룹 이름 변경 (새 이름이 있을 때만)
            if (dto.getNewAccessGroupName() != null && !dto.getNewAccessGroupName().isBlank()) {
                // 동일 워크스페이스 내 중복 이름 방지
                boolean exists = accessGroupRepository.existsByWorkspaceIdAndAccessGroupName(workspace.getId(), dto.getNewAccessGroupName());
                if (exists) {
                    throw new IllegalArgumentException("이미 동일한 이름의 권한 그룹이 존재합니다: " + dto.getNewAccessGroupName());
                }
                targetGroup.setAccessGroupName(dto.getNewAccessGroupName());
                accessGroupRepository.save(targetGroup);
            }

            // 5. 순회하면서 권한 여부 업데이트
            for (int i = 0; i < types.length; i++) {
                AccessType type = types[i];
                boolean newAccess = i < accessValues.size() && Boolean.TRUE.equals(accessValues.get(i));

                AccessDetail detail = existingDetails.stream()
                        .filter(d -> d.getAccessList().getAccessType() == type)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("AccessDetail이 누락되었습니다. type=" + type));

                detail.setAccess(newAccess);
                accessDetailRepository.save(detail);
            }

        } else {
//            공통모듈에서 에러코드 써야하는데 추후에 수정하겠습니다
            throw new IllegalArgumentException("관리자만 권한 그룹을 수정할 수 있습니다.");
        }


    }

    //    권한그룹 리스트 조회

    public Page<AccessGroupListResDto> accessGroupList(Pageable pageable, String userEmail, String workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없습니다. ID=" + workspaceId));
        Page<AccessGroup> accessGroupPage = accessGroupRepository.findByWorkspaceId(workspace.getId(), pageable);
        return accessGroupPage.map(
                group -> AccessGroupListResDto.fromEntity(group, workspaceParticipantRepository.countByAccessGroup(group))
        );
    }

    //    권한그룹 상세 조회

    //    권한그룹 사용자 추가

    //    권한그룹 사용자 수정

    //    권한그룹 사용자 제거

    //    권한그룹 삭제
}
