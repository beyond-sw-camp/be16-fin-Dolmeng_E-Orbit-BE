package com.Dolmeng_E.workspace.domain.access_group.service;

import com.Dolmeng_E.workspace.common.dto.UserIdListDto;
import com.Dolmeng_E.workspace.common.dto.UserInfoListResDto;
import com.Dolmeng_E.workspace.common.dto.UserInfoResDto;
import com.Dolmeng_E.workspace.common.service.UserFeign;
import com.Dolmeng_E.workspace.domain.access_group.dto.*;
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

import java.util.*;


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
    public static final String DEFAULT_GROUP_NAME = "일반 유저 그룹";
    public static final String ADMIN_GROUP_NAME = "관리자 그룹";

//    To-Do : 모든 로직 구현 후 try-catch 작업 해야함

    // 관리자 권한 그룹 생성 (워크스페이스 ID 기반, 워크스페이스 생성시 자동생성)
    public String createAdminGroupForWorkspace(String workspaceId) {

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없습니다. ID=" + workspaceId));

        AccessGroup adminGroup = AccessGroup.builder()
                .workspace(workspace)
                .accessGroupName(ADMIN_GROUP_NAME)
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
                .accessGroupName(DEFAULT_GROUP_NAME)
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

    public void createCustomAccessGroup(CustomAccessGroupDto dto, String userId) {


        // 1. 워크스페이스 조회
        Workspace workspace = workspaceRepository.findById(dto.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없습니다. ID=" + dto.getWorkspaceId()));
        // 2. 유저 권한 체크
        UserInfoResDto userInfoResDto = userFeign.fetchUserInfoById(userId);
        WorkspaceParticipant workspaceParticipant = workspaceParticipantRepository.findByWorkspaceIdAndUserId(workspace.getId(),userInfoResDto.getUserId())
                .orElseThrow(()->new EntityNotFoundException("워크스페이스 참여자를 찾을 수 없습니다."));
        if (workspaceParticipant.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {


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
        } else {
            //            공통모듈에서 에러코드 써야하는데 추후에 수정하겠습니다
            throw new IllegalArgumentException("관리자만 권한 그룹을 수정할 수 있습니다.");
        }
    }

    //    권한그룹 수정

    public void modifyAccessGroup(AccessGroupModifyDto dto, String userId) {
        // 1. 워크스페이스 조회
        Workspace workspace = workspaceRepository.findById(dto.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없습니다. ID=" + dto.getWorkspaceId()));
        // 2. 유저 권한 체크
        UserInfoResDto userInfoResDto = userFeign.fetchUserInfoById(userId);
        WorkspaceParticipant workspaceParticipant = workspaceParticipantRepository.findByWorkspaceIdAndUserId(workspace.getId(),userInfoResDto.getUserId())
                .orElseThrow(()->new EntityNotFoundException("워크스페이스 참여자를 찾을 수 없습니다."));
        if (workspaceParticipant.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            // 1. 수정할 AccessGroup 조회
            AccessGroup targetGroup = accessGroupRepository.findByWorkspaceIdAndAccessGroupName(
                    workspace.getId(),
                    dto.getAccessGroupName()
            ).orElseThrow(() -> new EntityNotFoundException("수정 대상 권한 그룹을 찾을 수 없습니다."));

            // 기본 그룹은 수정 불가
            if (targetGroup.getAccessGroupName().equals(DEFAULT_GROUP_NAME)) {
                throw new IllegalArgumentException("기본 권한그룹은 삭제할 수 없습니다.");
            }
            // 관리자 그룹은 수정 불가
            if (targetGroup.getAccessGroupName().equals(ADMIN_GROUP_NAME)) {
                throw new IllegalArgumentException("관리자 권한그룹은 삭제할 수 없습니다.");
            }

            // 2. AccessType 목록 가져오기
            AccessType[] types = AccessType.values();
            List<Boolean> accessValues = dto.getAccessList();

            // 3. 기존 AccessDetail 목록 조회
            List<AccessDetail> existingDetails = accessDetailRepository.findByAccessGroup(targetGroup);

            // 4. 권한그룹 이름 변경 (새 이름이 있을 때만)
            if (dto.getNewAccessGroupName() != null && !dto.getNewAccessGroupName().isBlank()) {
                // 기존 권한 그룹명이 바꿀 그룹명과 같으면 패스, 다르면 중복이름 검사
                if(!targetGroup.getAccessGroupName().equals(dto.getNewAccessGroupName())) {
                    // 동일 워크스페이스 내 중복 이름 방지
                    boolean exists = accessGroupRepository.existsByWorkspaceIdAndAccessGroupName(workspace.getId(), dto.getNewAccessGroupName());
                    if (exists) {
                        throw new IllegalArgumentException("이미 동일한 이름의 권한 그룹이 존재합니다: " + dto.getNewAccessGroupName());
                    }
                    targetGroup.setAccessGroupName(dto.getNewAccessGroupName());
                    accessGroupRepository.save(targetGroup);
                }

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

    // 권한그룹 조회
    @Transactional(readOnly = true)
    public AccessGroupResDto accessGroup(String userId, String accessGroupId) {

        // 1️. 권한 그룹 조회
        AccessGroup accessGroup = accessGroupRepository.findById(accessGroupId)
                .orElseThrow(() -> new EntityNotFoundException("권한그룹이 존재하지 않습니다."));

        Workspace workspace = accessGroup.getWorkspace();

        // 2️. 요청자 유효성 검증 (워크스페이스 참여자 확인)
        WorkspaceParticipant requester = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        if (!requester.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            throw new IllegalArgumentException("관리자가 아닙니다.");
        }

        // 3️. 해당 그룹의 AccessDetail 목록 조회
        List<AccessDetail> details = accessDetailRepository.findByAccessGroup(accessGroup);

        // 4️. 권한별 boolean 세팅
        boolean projectCreate = false;
        boolean stoneCreate = false;
        boolean projectFileView = false;
        boolean stoneFileView = false;
        boolean workspaceFileView = false;

        // 5개 코딩해 둔 권한 목록에서 for문으로 테스트
        for (AccessDetail d : details) {
            // false라면 다음 권한으로 넘어갑니다.
            if (!d.getIsAccess()) continue;
            switch (d.getAccessList().getAccessType()) {
                case PROJECT_CREATE -> projectCreate = true;
                case STONE_CREATE -> stoneCreate = true;
                case PROJECT_FILE_VIEW -> projectFileView = true;
                case STONE_FILE_VIEW -> stoneFileView = true;
                case WORKSPACE_FILE_VIEW -> workspaceFileView = true;
            }
        }

        // 5️. DTO 조립
        return AccessGroupResDto.builder()
                .accessGroupId(accessGroup.getId())
                .accessGroupName(accessGroup.getAccessGroupName())
                .projectCreate(projectCreate)
                .stoneCreate(stoneCreate)
                .projectFileView(projectFileView)
                .stoneFileView(stoneFileView)
                .workspaceFileView(workspaceFileView)
                .build();
    }





    //    권한그룹 리스트 조회

    public Page<AccessGroupListResDto> accessGroupList(Pageable pageable, String userId, String workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없습니다. ID=" + workspaceId));
        Page<AccessGroup> accessGroupPage = accessGroupRepository.findByWorkspaceId(workspace.getId(), pageable);
        return accessGroupPage.map(
                group -> AccessGroupListResDto.fromEntity(group, workspaceParticipantRepository.countByAccessGroup(group))
        );
    }

    //    권한그룹 상세 조회

    @Transactional(readOnly = true)
    public AccessGroupUserListResDto  getAccessGroupDetail(String userId, String groupId) {

        // 1. 권한그룹 조회
        AccessGroup accessGroup = accessGroupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("해당 권한그룹이 존재하지 않습니다."));

        Workspace workspace = accessGroup.getWorkspace();

        // 2. 요청자(관리자) 확인
        UserInfoResDto adminInfo = userFeign.fetchUserInfoById(userId);
        WorkspaceParticipant admin = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), adminInfo.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("요청자는 워크스페이스 참가자가 아닙니다."));

        if (!admin.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            throw new IllegalArgumentException("관리자만 권한그룹 상세를 조회할 수 있습니다.");
        }

        // 3. 해당 그룹 참여자 조회
        List<WorkspaceParticipant> participants = workspaceParticipantRepository.findByAccessGroup(accessGroup);

        if (participants.isEmpty()) {
            return AccessGroupUserListResDto.builder()
                    .groupId(accessGroup.getId())
                    .groupName(accessGroup.getAccessGroupName())
                    .userList(List.of())
                    .build();
        }

        // 4. 유저 ID 리스트 구성
        List<UUID> userIds = participants.stream()
                .map(WorkspaceParticipant::getUserId)
                .toList();

        // 5. UserFeign 호출 → 사용자 상세 정보 조회
        UserIdListDto userIdListDto = new UserIdListDto(userIds);
        UserInfoListResDto userInfoListResDto = userFeign.fetchUserListInfo(userIdListDto);

        // 6. 사용자 정보 + 역할 결합
        List<AccessGroupUserDetailDto> detailedUserList = participants.stream()
                .map(participant -> {
                    UserInfoResDto userInfo = userInfoListResDto.getUserInfoList().stream()
                            .filter(u -> u.getUserId().equals(participant.getUserId()))
                            .findFirst()
                            .orElse(null);

                    return AccessGroupUserDetailDto.builder()
                            .userInfo(userInfo)
                            .workspaceRole(participant.getWorkspaceRole())
                            .build();
                })
                .toList();

        // 7. 반환 DTO 구성
        return AccessGroupUserListResDto.builder()
                .groupId(accessGroup.getId())
                .groupName(accessGroup.getAccessGroupName())
                .userList(detailedUserList)
                .build();

    }

    //    권한그룹 사용자 추가

    public void addUserToAccessGroup(String userId, String groupId, AccessGroupAddUserDto accessGroupAddUserDto) {
        // 1. 워크스페이스 조회
        AccessGroup accessGroup = accessGroupRepository.findById(groupId).orElseThrow(()->new EntityNotFoundException("해당 권한그룹 없습니다."));
        Workspace workspace = workspaceRepository.findById(accessGroup.getWorkspace().getId())
                .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없습니다. ID=" + accessGroup.getWorkspace().getId()));
        // 2. 유저 워크스페이스 참여자 체크
        UserInfoResDto userInfoResDto = userFeign.fetchUserInfoById(userId);
        WorkspaceParticipant workspaceParticipant = workspaceParticipantRepository.findByWorkspaceIdAndUserId(workspace.getId(),userInfoResDto.getUserId())
                .orElseThrow(()->new EntityNotFoundException("워크스페이스 참여자를 찾을 수 없습니다."));

        // 3. 유저 리스트
        UserIdListDto userIdListDto = new UserIdListDto(accessGroupAddUserDto.getUserIdList());
        UserInfoListResDto userInfoListResDto = userFeign.fetchUserListInfo(userIdListDto);

        if (workspaceParticipant.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            for (UserInfoResDto userInfo : userInfoListResDto.getUserInfoList()) {
                workspaceParticipantRepository.save(
                        WorkspaceParticipant.builder()
                                .workspaceRole(WorkspaceRole.COMMON)
                                .accessGroup(accessGroup)
                                .userId(userInfo.getUserId())
                                .isDelete(false)
                                .workspace(workspace)
                                .userName(userInfo.getUserName())
                                .build()
                );
            }

        } else {
            //            공통모듈에서 에러코드 써야하는데 추후에 수정하겠습니다
            throw new IllegalArgumentException("관리자만 권한 그룹을 수정할 수 있습니다.");
        }


    }

    // 권한그룹 사용자 변경 (워크스페이스 내 기존 사용자 그룹 변경)
    @Transactional
    public void updateUserAccessGroup(String userId, String groupId, AccessGroupAddUserDto dto) {

        // 1. 권한그룹 조회
        AccessGroup accessGroup = accessGroupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("해당 권한그룹이 존재하지 않습니다."));
        Workspace workspace = accessGroup.getWorkspace();

        // 2. 요청자(관리자) 검증
        UserInfoResDto adminInfo = userFeign.fetchUserInfoById(userId);
        WorkspaceParticipant admin = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), adminInfo.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("요청자는 워크스페이스 참가자가 아닙니다."));
        if (!admin.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            throw new IllegalArgumentException("관리자만 권한그룹을 수정할 수 있습니다.");
        }

        // 3. 기본 그룹 조회 (fallback용)
        AccessGroup defaultGroup = accessGroupRepository
                .findByWorkspaceIdAndAccessGroupName(workspace.getId(), DEFAULT_GROUP_NAME)
                .orElseThrow(() -> new EntityNotFoundException("기본 권한그룹이 존재하지 않습니다."));

        // 4. 현재 그룹 인원 조회
        List<WorkspaceParticipant> currentParticipants =
                workspaceParticipantRepository.findByWorkspaceAndAccessGroup(workspace, accessGroup);

        // 5. 요청 목록이 비어있을 경우 → 전체를 기본 그룹으로 이동
        if (dto.getUserIdList() == null || dto.getUserIdList().isEmpty()) {
            for (WorkspaceParticipant p : currentParticipants) {
                if (p.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) continue;
                p.setAccessGroup(defaultGroup);
                workspaceParticipantRepository.save(p);
            }
            return;
        }

        // 6. 요청된 사용자 ID set 생성
        Set<UUID> requestedUserIds = new HashSet<>(dto.getUserIdList());

        // 7. 요청 목록에 없는 기존 인원 → 기본 그룹으로 이동
        for (WorkspaceParticipant p : currentParticipants) {
            if (p.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) continue;
            if (!requestedUserIds.contains(p.getUserId())) {
                p.setAccessGroup(defaultGroup);
                workspaceParticipantRepository.save(p);
            }
        }

        // 8. 요청된 인원 → 지정된 권한그룹으로 이동
        UserIdListDto userIdListDto = new UserIdListDto(dto.getUserIdList());
        UserInfoListResDto userInfoListResDto = userFeign.fetchUserListInfo(userIdListDto);

        for (UserInfoResDto userInfo : userInfoListResDto.getUserInfoList()) {
            WorkspaceParticipant participant = workspaceParticipantRepository
                    .findByWorkspaceIdAndUserId(workspace.getId(), userInfo.getUserId())
                    .orElseThrow(() -> new EntityNotFoundException("해당 사용자는 워크스페이스에 존재하지 않습니다."));

            if (participant.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) continue;

            participant.setAccessGroup(accessGroup);
            workspaceParticipantRepository.save(participant);
        }
    }



    //    권한그룹 사용자 이동(일반 그룹으로 이동)
    public void moveUserAccessGroup(String userId, String groupId, AccessGroupMoveDto dto) {
        // 1. 권한그룹 조회
        AccessGroup accessGroup = accessGroupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("해당 권한그룹이 존재하지 않습니다."));

        Workspace workspace = accessGroup.getWorkspace();

        // 2. 요청자(관리자) 확인
        UserInfoResDto adminInfo = userFeign.fetchUserInfoById(userId);
        WorkspaceParticipant admin = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), adminInfo.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("요청자는 워크스페이스 참가자가 아닙니다."));

        if (!admin.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            throw new IllegalArgumentException("관리자만 권한그룹을 수정할 수 있습니다.");
        }

        // 3. 기본 그룹 조회 (권한그룹명 + workspaceId로 찾기)
        AccessGroup defaultGroup = accessGroupRepository
                .findByWorkspaceIdAndAccessGroupName(workspace.getId(), DEFAULT_GROUP_NAME)
                .orElseThrow(() -> new EntityNotFoundException("기본 권한그룹이 존재하지 않습니다."));

        // 4. 대상 유저 리스트 조회
        UserIdListDto userIdListDto = new UserIdListDto(dto.getUserIdList());
        UserInfoListResDto userInfoListResDto = userFeign.fetchUserListInfo(userIdListDto);

        // 5. 그룹 이동 처리
        for (UserInfoResDto userInfo : userInfoListResDto.getUserInfoList()) {
            WorkspaceParticipant participant = workspaceParticipantRepository
                    .findByWorkspaceIdAndUserId(workspace.getId(), userInfo.getUserId())
                    .orElseThrow(() -> new EntityNotFoundException("해당 사용자는 워크스페이스에 존재하지 않습니다."));

            // 관리자면 스킵
            if (participant.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
                continue;
            }

            // 기본 그룹으로 이동
            participant.setAccessGroup(defaultGroup);
            workspaceParticipantRepository.save(participant);


        }
    }

    //    권한그룹 삭제

    public void deleteUserAccessGroup(String userId, String groupId) {
        // 1. 권한그룹 조회
        AccessGroup accessGroup = accessGroupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("해당 권한그룹이 존재하지 않습니다."));

        Workspace workspace = accessGroup.getWorkspace();

        // 2. 요청자(관리자) 확인
        UserInfoResDto adminInfo = userFeign.fetchUserInfoById(userId);
        WorkspaceParticipant admin = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), adminInfo.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("요청자는 워크스페이스 참가자가 아닙니다."));

        if (!admin.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            throw new IllegalArgumentException("관리자만 권한그룹을 삭제할 수 있습니다.");
        }

        // 3. 기본 그룹 조회
        AccessGroup defaultGroup = accessGroupRepository
                .findByWorkspaceIdAndAccessGroupName(workspace.getId(), DEFAULT_GROUP_NAME)
                .orElseThrow(() -> new EntityNotFoundException("기본 권한그룹이 존재하지 않습니다."));

        // 기본 그룹은 삭제 불가
        if (accessGroup.getAccessGroupName().equals(DEFAULT_GROUP_NAME)) {
            throw new IllegalArgumentException("기본 권한그룹은 삭제할 수 없습니다.");
        }
        // 관리자 그룹은 삭제 불가
        if (accessGroup.getAccessGroupName().equals(ADMIN_GROUP_NAME)) {
            throw new IllegalArgumentException("관리자 권한그룹은 삭제할 수 없습니다.");
        }

        // 4. 해당 그룹에 속한 사용자들 조회
        List<WorkspaceParticipant> participants = workspaceParticipantRepository.findByAccessGroup(accessGroup);

        // 5. 기본 그룹으로 이동
        for (WorkspaceParticipant participant : participants) {
            if (participant.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
                continue; // 관리자 건너뛰기
            }

            participant.setAccessGroup(defaultGroup);
            workspaceParticipantRepository.save(participant);
        }

        // 6. 그룹 삭제
        accessDetailRepository.deleteAllByAccessGroup(accessGroup);
        accessGroupRepository.delete(accessGroup);
    }

    // 본인 권한목록 조회 api
    public MyAccessGroupResDto getMyAccess(String userId, String workspaceId) {
        // 1) 워크스페이스 검증
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스가 존재하지 않습니다."));

        // 2) 참여자 검증 (userId는 UUID이면 변환 필요)
        WorkspaceParticipant workspaceParticipant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspaceId, UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 3) 참여자의 AccessGroup 조회 (없으면 전부 false)
        AccessGroup accessGroup = workspaceParticipant.getAccessGroup();
        if (accessGroup == null) {
            return MyAccessGroupResDto.builder()
                    .isProjectCreate(false)
                    .isStoneCreate(false)
                    .isProjectFileView(false)
                    .isStoneFileView(false)
                    .isWorkspaceFileView(false)
                    .build();
        }

        // 4) 그룹의 AccessDetail 전부 조회 (AccessList 조인됨)
        List<AccessDetail> details = accessDetailRepository
                .findAllByAccessGroupIdWithAccessList(accessGroup.getId());

        // 5) AccessType → boolean 매핑 테이블 구축 (기본 false)
        Map<AccessType, Boolean> map = new EnumMap<>(AccessType.class);
        for (AccessType t : AccessType.values()) map.put(t, false);

        for (AccessDetail d : details) {
            // AccessList가 반드시 있어야 함
            if (d.getAccessList() != null && d.getAccessList().getAccessType() != null) {
                map.put(d.getAccessList().getAccessType(), Boolean.TRUE.equals(d.getIsAccess()));
            }
        }

        // 6) DTO로 변환
        return MyAccessGroupResDto.builder()
                .isProjectCreate(map.getOrDefault(AccessType.PROJECT_CREATE, false))
                .isStoneCreate(map.getOrDefault(AccessType.STONE_CREATE, false))
                .isProjectFileView(map.getOrDefault(AccessType.PROJECT_FILE_VIEW, false))
                .isStoneFileView(map.getOrDefault(AccessType.STONE_FILE_VIEW, false))
                .isWorkspaceFileView(map.getOrDefault(AccessType.WORKSPACE_FILE_VIEW, false))
                .build();
    }


}
