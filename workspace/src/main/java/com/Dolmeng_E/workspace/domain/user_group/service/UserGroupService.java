package com.Dolmeng_E.workspace.domain.user_group.service;

import com.Dolmeng_E.workspace.common.dto.UserIdListDto;
import com.Dolmeng_E.workspace.common.dto.UserInfoListResDto;
import com.Dolmeng_E.workspace.common.dto.UserInfoResDto;
import com.Dolmeng_E.workspace.common.service.AccessCheckService;
import com.Dolmeng_E.workspace.common.service.UserFeign;
import com.Dolmeng_E.workspace.domain.user_group.dto.*;
import com.Dolmeng_E.workspace.domain.user_group.entity.UserGroup;
import com.Dolmeng_E.workspace.domain.user_group.entity.UserGroupMapping;
import com.Dolmeng_E.workspace.domain.user_group.repository.UserGroupMappingRepository;
import com.Dolmeng_E.workspace.domain.user_group.repository.UserGroupRepository;
import com.Dolmeng_E.workspace.domain.workspace.entity.Workspace;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceRole;
import com.Dolmeng_E.workspace.domain.workspace.repository.WorkspaceParticipantRepository;
import com.Dolmeng_E.workspace.domain.workspace.repository.WorkspaceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class UserGroupService {
    private final UserGroupRepository userGroupRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceParticipantRepository workspaceParticipantRepository;
    private final UserGroupMappingRepository userGroupMappingRepository;
    private final UserFeign userFeign;
    private final AccessCheckService accessCheckService;

    // 사용자 그룹 생성
    public String createUserGroup(String userId, UserGroupCreateDto dto) {
        // 1️. 워크스페이스 조회
        Workspace workspace = workspaceRepository.findById(dto.getWorkspaceId())
                .orElseThrow(() -> new EntityNotFoundException("해당 워크스페이스가 존재하지 않습니다."));

        // 2️. 관리자 혹은 사용자 그룹 권한 있는지 확인
        UserInfoResDto adminInfo = userFeign.fetchUserInfoById(userId);
        WorkspaceParticipant requester = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), adminInfo.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("요청자는 워크스페이스 참가자가 아닙니다."));

        if (!requester.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            // 관리자 아니면 권한 검증 (내부에서 예외 발생 시 catch 없이 자동 전파)
            throw new IllegalArgumentException("관리자 권한이 필요합니다.");
        }

        if (dto.getUserGroupName() == null || dto.getUserGroupName().isBlank()) {
            throw new IllegalArgumentException("사용자 그룹명이 없습니다.");
        }


        // 3️. 동일 워크스페이스 내 중복 그룹명 검증
        boolean exists = userGroupRepository.existsByWorkspaceAndUserGroupName(
                workspace,
                dto.getUserGroupName()
        );

        if (exists) {
            throw new DuplicateKeyException("해당 워크스페이스 내 동일한 사용자 그룹명이 이미 존재합니다.");
        }

        // 4️. 그룹 생성
        UserGroup userGroup = UserGroup.builder()
                .workspace(workspace)
                .userGroupName(dto.getUserGroupName())
                .build();

        userGroupRepository.save(userGroup);

        // 5. 중복 방지 - 이미 그룹에 속한 유저 제외(프론트에서 사용자그룹이 없는 유저 목록을 제공하기 때문에 에러없이 db저장만 막음)
        Set<UUID> existingUserIds = userGroupMappingRepository
                .findByUserGroup(userGroup)
                .stream()
                .map(mapping -> mapping.getWorkspaceParticipant().getUserId())
                .collect(Collectors.toSet());

        // 사용자 목록이 비었을 때 예외 처리
        if (dto.getUserIdList() == null || dto.getUserIdList().isEmpty()) {
            throw new IllegalArgumentException("추가할 유저 목록이 없습니다.");
        }

        // 6. 유저 매핑 저장
        for (UUID id : dto.getUserIdList()) {
            if (existingUserIds.contains(id)) continue; // 중복 제외

            WorkspaceParticipant participant = workspaceParticipantRepository
                    .findByWorkspaceIdAndUserId(workspace.getId(), id)
                    .orElseThrow(() -> new EntityNotFoundException("워크스페이스에 속하지 않은 사용자입니다."));

            userGroupMappingRepository.save(
                    UserGroupMapping.builder()
                            .userGroup(userGroup)
                            .workspaceParticipant(participant)
                            .build()
            );
        }

        return userGroup.getId();
    }

    // 사용자 그룹 목록 조회
    public Page<UserGroupListResDto> getUserGroupList(String userId, String workspaceId, Pageable pageable) {

        // 1️. 워크스페이스 조회
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("해당 워크스페이스가 존재하지 않습니다."));

        // 2️. 요청자 검증(사용자 그룹 목록 조회는 여러 서비스에서 필요하기 때문에 권한 x)
        UserInfoResDto adminInfo = userFeign.fetchUserInfoById(userId);
        WorkspaceParticipant requester = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), adminInfo.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("요청자는 워크스페이스 참가자가 아닙니다."));

        // 3. 그룹 목록 조회 (페이지네이션)
        Page<UserGroup> groups = userGroupRepository.findByWorkspace(workspace, pageable);

        // 4. 참여자 수 계산 + DTO 변환
        return groups.map(group -> UserGroupListResDto.builder()
                .groupId(group.getId())
                .groupName(group.getUserGroupName())
                .createdAt(group.getCreatedAt())
                .participantCount(userGroupMappingRepository.countByUserGroup(group))
                .build());

    }

    // 사용자 그룹에 추가
    public void addUsersToGroup(String userId, String groupId, UserGroupAddUserDto dto) {
        // 1. 그룹 조회
        UserGroup userGroup = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("해당 사용자 그룹이 존재하지 않습니다."));

        Workspace workspace = userGroup.getWorkspace();

        // 2️. 관리자 혹은 사용자 그룹 권한 있는지 확인
        UserInfoResDto adminInfo = userFeign.fetchUserInfoById(userId);
        WorkspaceParticipant requester = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), adminInfo.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("요청자는 워크스페이스 참가자가 아닙니다."));

        if (!requester.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            // 관리자 아니면 권한 검증 (내부에서 예외 발생 시 catch 없이 자동 전파)
            throw new IllegalArgumentException("관리자 권한이 필요합니다.");
        }

        // 3. 유저 존재 검증 (Feign)
        UserIdListDto userIdListDto = new UserIdListDto(dto.getUserIdList());
        UserInfoListResDto userInfoListResDto = userFeign.fetchUserListInfo(userIdListDto);

        // 4. 중복 방지 - 이미 그룹에 속한 유저 제외
        Set<UUID> existingUserIds = userGroupMappingRepository
                .findByUserGroup(userGroup)
                .stream()
                .map(mapping -> mapping.getWorkspaceParticipant().getUserId())
                .collect(Collectors.toSet());

        // 5. 유저 매핑 저장
        for (UserInfoResDto userInfo : userInfoListResDto.getUserInfoList()) {
            if (existingUserIds.contains(userInfo.getUserId())) continue; // 중복 제외

            WorkspaceParticipant participant = workspaceParticipantRepository
                    .findByWorkspaceIdAndUserId(workspace.getId(), userInfo.getUserId())
                    .orElseThrow(() -> new EntityNotFoundException("워크스페이스에 속하지 않은 사용자입니다."));

            userGroupMappingRepository.save(
                    UserGroupMapping.builder()
                            .userGroup(userGroup)
                            .workspaceParticipant(participant)
                            .build()
            );
        }
    }



    // 사용자 그룹 상세 조회
    public UserGroupDetailResDto getUserGroupDetail(String userId, String groupId, Pageable pageable) {

        // 1. 사용자 그룹 조회
        UserGroup userGroup = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("해당 사용자 그룹이 존재하지 않습니다."));

        Workspace workspace = userGroup.getWorkspace();

        // 2️. 관리자 혹은 사용자 그룹 권한 있는지 확인
        UserInfoResDto adminInfo = userFeign.fetchUserInfoById(userId);
        WorkspaceParticipant requester = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), adminInfo.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("요청자는 워크스페이스 참가자가 아닙니다."));

        if (!requester.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            // 관리자 아니면 권한 검증 (내부에서 예외 발생 시 catch 없이 자동 전파)
            throw new IllegalArgumentException("관리자 권한이 필요합니다.");
        }

        // 3. 그룹 내 매핑된 사용자 목록 (페이징)
        Page<UserGroupMapping> mappings = userGroupMappingRepository.findByUserGroup(userGroup, pageable);

        if (mappings.isEmpty()) {
            return UserGroupDetailResDto.builder()
                    .groupId(userGroup.getId())
                    .groupName(userGroup.getUserGroupName())
                    .members(Page.empty())
                    .build();
        }

        // 4. 매핑에서 userId 추출
        List<UUID> userIds = mappings.stream()
                .map(mapping -> mapping.getWorkspaceParticipant().getUserId())
                .toList();

        // 5. 사용자 상세 정보 조회 (Feign)
        UserInfoListResDto userInfoListResDto = userFeign.fetchUserListInfo(new UserIdListDto(userIds));

        // 6. 사용자 정보 + 매핑 결합
        List<UserGroupMemberDto> memberDtos = mappings.stream()
                .map(mapping -> {
                    UUID id = mapping.getWorkspaceParticipant().getUserId();
                    UserInfoResDto userInfo = userInfoListResDto.getUserInfoList().stream()
                            .filter(u -> u.getUserId().equals(id))
                            .findFirst()
                            .orElse(null);

                    return UserGroupMemberDto.builder()
                            .userId(id)
                            .userName(userInfo != null ? userInfo.getUserName() : "유저 이름 없음")
                            .userEmail(userInfo != null ? userInfo.getUserEmail() : "유저 이메일 없음")
                            .profileImageUrl(userInfo != null ? userInfo.getProfileImageUrl() : null)
                            .build();
                })
                .toList();

        // 7. DTO 페이지 변환
        Page<UserGroupMemberDto> memberPage =
                new PageImpl<>(memberDtos, pageable, mappings.getTotalElements());

        // 8. 최종 반환
        return UserGroupDetailResDto.builder()
                .groupId(userGroup.getId())
                .groupName(userGroup.getUserGroupName())
                .members(memberPage)
                .build();
    }


    // 사용자를 그룹에서 삭제
    public void removeUsersFromGroup(String userId, String groupId, UserGroupRemoveUserDto dto) {

        // 1. 사용자 그룹 조회
        UserGroup userGroup = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("해당 사용자 그룹이 존재하지 않습니다."));

        Workspace workspace = userGroup.getWorkspace();

        // 2️. 관리자 혹은 사용자 그룹 권한 있는지 확인
        UserInfoResDto adminInfo = userFeign.fetchUserInfoById(userId);
        WorkspaceParticipant requester = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), adminInfo.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("요청자는 워크스페이스 참가자가 아닙니다."));

        if (!requester.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            // 관리자 아니면 권한 검증 (내부에서 예외 발생 시 catch 없이 자동 전파)
            throw new IllegalArgumentException("관리자 권한이 필요합니다.");
        }

        // 3. 요청 값 유효성 검사
        List<UUID> userIdList = dto.getUserIdList();
        if (userIdList == null || userIdList.isEmpty()) {
            throw new IllegalArgumentException("삭제할 사용자 목록이 비어있습니다.");
        }

        // 4. 실제 그룹에 속한 사용자 매핑 조회
        List<UserGroupMapping> existingMappings =
                userGroupMappingRepository.findAllByUserGroup(userGroup);

        // 그룹에 속한 실제 사용자 ID들
        Set<UUID> groupUserIds = existingMappings.stream()
                .map(mapping -> mapping.getWorkspaceParticipant().getUserId())
                .collect(Collectors.toSet());

        // 5. 요청한 사용자 중 그룹에 없는 사용자 필터링
        List<UUID> invalidUserIds = userIdList.stream()
                .filter(id -> !groupUserIds.contains(id))
                .toList();

        if (!invalidUserIds.isEmpty()) {
            throw new IllegalArgumentException("그룹에 속하지 않은 사용자가 포함되어 있습니다: " + invalidUserIds);
        }

        // 6. 실제 워크스페이스 참가자인지도 한번 더 검증 (데이터 무결성 보강)
        List<WorkspaceParticipant> workspaceParticipants =
                workspaceParticipantRepository.findByWorkspaceId(workspace.getId());
        Set<UUID> workspaceUserIds = workspaceParticipants.stream()
                .map(WorkspaceParticipant::getUserId)
                .collect(Collectors.toSet());

        List<UUID> notWorkspaceUsers = userIdList.stream()
                .filter(id -> !workspaceUserIds.contains(id))
                .toList();

        if (!notWorkspaceUsers.isEmpty()) {
            throw new IllegalArgumentException("워크스페이스에 존재하지 않는 사용자가 포함되어 있습니다: " + notWorkspaceUsers);
        }

        // 7. 검증 완료 후 안전 삭제
        userGroupMappingRepository.deleteByUserGroupAndWorkspaceParticipant_UserIdIn(userGroup, userIdList);
    }

    // 사용자 그룹 삭제
    public void deleteUserGroup(String userId, String groupId) {

        // 1. 그룹 조회
        UserGroup userGroup = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("해당 사용자 그룹이 존재하지 않습니다."));

        Workspace workspace = userGroup.getWorkspace();

        // 2️. 관리자 혹은 사용자 그룹 권한 있는지 확인
        UserInfoResDto adminInfo = userFeign.fetchUserInfoById(userId);
        WorkspaceParticipant requester = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), adminInfo.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("요청자는 워크스페이스 참가자가 아닙니다."));

        if (!requester.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            // 관리자 아니면 권한 검증 (내부에서 예외 발생 시 catch 없이 자동 전파)
            throw new IllegalArgumentException("관리자 권한이 필요합니다.");
        }

        // 3. 그룹 내 매핑된 유저 삭제 (Cascade 대신 명시적으로)
        userGroupMappingRepository.deleteAllByUserGroup(userGroup);

        // 4. 그룹 삭제
        userGroupRepository.delete(userGroup);
    }

    // 사용자 그룹 그룹명으로 검색
    @Transactional(readOnly = true)
    public Page<UserGroupSearchRestDto> SearchByGroupName(String userId, UserGroupSearchDto dto, Pageable pageable) {

        // 1. 워크스페이스 조회
        Workspace workspace = workspaceRepository.findById(dto.getWorkspaceId())
                .orElseThrow(() -> new EntityNotFoundException("해당 워크스페이스가 존재하지 않습니다."));

        // 2. 요청자 검증
        UserInfoResDto userInfo = userFeign.fetchUserInfoById(userId);
        WorkspaceParticipant requester = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), userInfo.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("요청자는 워크스페이스 참가자가 아닙니다."));

        // 3. 권한 검증
        if (!requester.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            throw new IllegalArgumentException("관리자 권한이 필요합니다.");
        }

        // 4. 검색어 검증
        String keyword = dto.getGroupName();
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("검색어가 비어있습니다.");
        }

        // 5. 그룹명 부분 일치 검색 (대소문자 구분 없음)
        Page<UserGroup> groups = userGroupRepository
                .findByWorkspaceAndUserGroupNameContainingIgnoreCase(workspace, keyword, pageable);

        // 6. 검색 결과 없을 경우 빈 페이지 반환
        List<UserGroup> groupList = groups.getContent();
        if (groupList.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        // 7. 조회된 그룹들에 속한 모든 매핑 한 번에 조회 (IN 쿼리 사용)
        List<UserGroupMapping> allMappings = userGroupMappingRepository.findByUserGroupIn(groupList);

        // 8. 전체 참여자 userId 수집
        List<UUID> allUserIds = allMappings.stream()
                .map(mapping -> mapping.getWorkspaceParticipant().getUserId())
                .distinct()
                .toList();

        // 9. 유저 상세 정보 조회 (Feign 한 번 호출)
        final Map<UUID, UserInfoResDto> userInfoMap = new HashMap<>();
        if (!allUserIds.isEmpty()) {
            UserInfoListResDto userInfoListResDto = userFeign.fetchUserListInfo(new UserIdListDto(allUserIds));
            userInfoListResDto.getUserInfoList().forEach(
                    user -> userInfoMap.put(user.getUserId(), user)
            );
        }

        // 10. 그룹별 매핑 묶기 (groupId 기준)
        Map<String, List<UserGroupMapping>> groupIdToMappings = allMappings.stream()
                .collect(Collectors.groupingBy(mapping -> mapping.getUserGroup().getId()));

        // 11. DTO 변환
        return groups.map(group -> {

            // 11-1. 그룹에 해당하는 매핑 목록 추출
            List<UserGroupMapping> groupMappings = groupIdToMappings.getOrDefault(group.getId(), List.of());

            // 11-2. 참여자 상세 정보 구성
            List<UserInfoResDto> participants = groupMappings.stream()
                    .map(mapping -> userInfoMap.get(mapping.getWorkspaceParticipant().getUserId()))
                    .filter(Objects::nonNull)
                    .toList();

            // 11-3. DTO 변환 및 반환
            return UserGroupSearchRestDto.builder()
                    .userGroupName(group.getUserGroupName())
                    .groupName(group.getUserGroupName()) // dto 구조 맞추기용
                    .createdAt(group.getCreatedAt())
                    .userGroupParticipantsCount(groupMappings.size())
                    .participants(participants)
                    .build();
        });
    }


    @Transactional
    public String modifyUserGroup(String userId, UserGroupModifyDto dto) {

        // 1️. 사용자 그룹 조회
        UserGroup userGroup = userGroupRepository.findById(dto.getUserGroupId())
                .orElseThrow(() -> new EntityNotFoundException("사용자 그룹 ID가 유효하지 않습니다."));
        Workspace workspace = userGroup.getWorkspace();

        // 2️. 관리자 권한 검증
        UserInfoResDto adminInfo = userFeign.fetchUserInfoById(userId);
        WorkspaceParticipant requester = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), adminInfo.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("요청자는 워크스페이스 참가자가 아닙니다."));

        if (!requester.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            throw new IllegalArgumentException("관리자 권한이 필요합니다.");
        }

        // 1. 그룹명 수정 처리
        if (dto.getUserGroupName() != null && !dto.getUserGroupName().isBlank()) {

            // 중복 그룹명 검증 (자기 자신은 예외)
            boolean exists = userGroupRepository.existsByWorkspaceAndUserGroupName(workspace, dto.getUserGroupName());
            if (exists && !userGroup.getUserGroupName().equals(dto.getUserGroupName())) {
                throw new DuplicateKeyException("해당 워크스페이스 내 동일한 사용자 그룹명이 이미 존재합니다.");
            }

            // 이름 변경
            userGroup.updateName(dto.getUserGroupName());
        }

        // 2. 참여자 수정 처리
        // userIdList가 null이거나 비어있으면 → 그룹 비우기
        if (dto.getUserIdList() == null || dto.getUserIdList().isEmpty()) {
            // 프론트에서 모든 참여자를 제거한 상태로 수정 요청 시
            // 그룹 내 기존 매핑 전체 삭제
            userGroupMappingRepository.deleteAllByUserGroup(userGroup);
        } else {
            // 기존 매핑 전체 삭제 후 새 참여자 추가
            userGroupMappingRepository.deleteAllByUserGroup(userGroup);

            for (UUID id : dto.getUserIdList()) {
                WorkspaceParticipant participant = workspaceParticipantRepository
                        .findByWorkspaceIdAndUserId(workspace.getId(), id)
                        .orElseThrow(() -> new EntityNotFoundException("워크스페이스에 속하지 않은 사용자입니다."));

                userGroupMappingRepository.save(
                        UserGroupMapping.builder()
                                .userGroup(userGroup)
                                .workspaceParticipant(participant)
                                .build()
                );
            }
        }


        userGroupRepository.save(userGroup);
        return userGroup.getId();
    }


}
