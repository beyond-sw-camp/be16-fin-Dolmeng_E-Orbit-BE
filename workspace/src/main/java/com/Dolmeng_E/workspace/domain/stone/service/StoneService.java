package com.Dolmeng_E.workspace.domain.stone.service;

import com.Dolmeng_E.workspace.common.dto.UserIdListDto;
import com.Dolmeng_E.workspace.common.dto.UserInfoListResDto;
import com.Dolmeng_E.workspace.common.dto.UserInfoResDto;
import com.Dolmeng_E.workspace.common.service.AccessCheckService;
import com.Dolmeng_E.workspace.common.service.MilestoneCalculator;
import com.Dolmeng_E.workspace.common.service.UserFeign;
import com.Dolmeng_E.workspace.domain.project.entity.Project;
import com.Dolmeng_E.workspace.domain.project.entity.ProjectParticipant;
import com.Dolmeng_E.workspace.domain.project.repository.ProjectParticipantRepository;
import com.Dolmeng_E.workspace.domain.stone.dto.*;
import com.Dolmeng_E.workspace.domain.stone.entity.StoneParticipant;
import com.Dolmeng_E.workspace.domain.project.repository.ProjectRepository;
import com.Dolmeng_E.workspace.domain.stone.repository.StoneParticipantRepository;
import com.Dolmeng_E.workspace.domain.stone.entity.ChildStoneList;
import com.Dolmeng_E.workspace.domain.stone.entity.Stone;
import com.Dolmeng_E.workspace.domain.stone.entity.StoneStatus;
import com.Dolmeng_E.workspace.domain.stone.repository.ChildStoneListRepository;
import com.Dolmeng_E.workspace.domain.stone.repository.StoneRepository;
import com.Dolmeng_E.workspace.domain.task.entity.Task;
import com.Dolmeng_E.workspace.domain.task.repository.TaskRepository;
import com.Dolmeng_E.workspace.domain.workspace.entity.Workspace;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceRole;
import com.Dolmeng_E.workspace.domain.workspace.repository.WorkspaceParticipantRepository;
import com.Dolmeng_E.workspace.domain.workspace.repository.WorkspaceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class StoneService {
    private final ChildStoneListRepository childStoneListRepository;
    private final StoneRepository stoneRepository;
    private final AccessCheckService accessCheckService;
    private final WorkspaceParticipantRepository workspaceParticipantRepository;
    private final ProjectRepository projectRepository;
    private final StoneParticipantRepository stoneParticipantRepository;
    private final ProjectParticipantRepository projectParticipantRepository;
    private final WorkspaceRepository workspaceRepository;
    private final TaskRepository taskRepository;
    private final UserFeign userFeign;
    private final MilestoneCalculator milestoneCalculator;

// 최상위 스톤 생성(프로젝트 생성 시 자동 생성)
    public String createTopStone(TopStoneCreateDto dto) {

        // 1. 참여자 검증
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findById(dto.getParticipantId())
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 2. 프로젝트 조회
        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다."));

        /*
        프로젝트 생성시 자동으로 호출되는 API기 때문에 권한검증은 따로 하지 않았습니다.
        if (!project.getWorkspaceParticipant().getId().equals(participant.getId())) {
            accessCheckService.validateAccess(participant, "ws_acc_list_3");
        }
        */

        // 스톤 담당자 프로젝트 참여자에 추가
        projectParticipantRepository.save(
                ProjectParticipant.builder()
                        .workspaceParticipant(participant)
                        .project(project)
                        .build()
        );

        // 3. 스톤생성 후 ID 리턴
        return stoneRepository.save( Stone.builder()
                .stoneName(dto.getStoneName())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .project(project)
                .stoneManager(participant) //스톤의 담당자
                .chatCreation(dto.getChatCreation() != null ? dto.getChatCreation() : false)
                .taskCreation(false) // 최상위 스톤은 태스크 x
                .milestone(dto.getMilestone() != null ? dto.getMilestone() : BigDecimal.ZERO) // 최초 마일스톤은 0퍼센트
                .status(StoneStatus.PROGRESS) // 최초 상태는 진행중으로 세팅
                .build()
        ).getId();
        //todo chatCreation이 true면 채팅방 생성 로직 추가해야함.

    }

    // 일반 스톤 생성
    public String createStone(String userId, StoneCreateDto dto) {

        // 1. 상위 스톤 조회
        Stone parentStone = stoneRepository.findById(dto.getParentStoneId())
                .orElseThrow(() -> new EntityNotFoundException("상위 스톤이 존재하지 않습니다."));

        // 2. 완료된 스톤에는 추가 불가
        if (parentStone.getStatus() == StoneStatus.COMPLETED) {
            throw new IllegalStateException("완료된 스톤에는 자식 스톤을 추가할 수 없습니다.");
        }

        // 3. 프로젝트, 워크스페이스 추적
        Project project = parentStone.getProject();
        Workspace workspace = project.getWorkspace();

        // 4. 요청자 검증
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 5. 권한 검증
        if (!participant.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            if (!project.getWorkspaceParticipant().getId().equals(participant.getId())) {
                accessCheckService.validateAccess(participant, "ws_acc_list_2");
            }
        }

        // 6. 스톤 기간 검증
        if (dto.getStartTime().isBefore(project.getStartTime()) ||
                dto.getEndTime().isAfter(project.getEndTime())) {
            throw new IllegalArgumentException("프로젝트 기간 내에만 스톤 생성이 가능합니다.");
        }

        // 7. 프로젝트 참가자 자동 등록
        if (dto.getParticipantIds() != null && !dto.getParticipantIds().isEmpty()) {
            for (UUID userUuid : dto.getParticipantIds()) {
                WorkspaceParticipant wp = workspaceParticipantRepository
                        .findByWorkspaceIdAndUserId(workspace.getId(), userUuid)
                        .orElseThrow(() -> new EntityNotFoundException("참여자 정보를 찾을 수 없습니다."));
                if (wp.getId().equals(participant.getId())) continue;

                if (!projectParticipantRepository.existsByProjectAndWorkspaceParticipant(project, wp)) {
                    projectParticipantRepository.save(
                            ProjectParticipant.builder()
                                    .project(project)
                                    .workspaceParticipant(wp)
                                    .build()
                    );
                }
            }
        }

        // 8. 스톤 담당자 프로젝트 참여자 등록
        if (!projectParticipantRepository.existsByProjectAndWorkspaceParticipant(project, participant)) {
            projectParticipantRepository.save(
                    ProjectParticipant.builder()
                            .workspaceParticipant(participant)
                            .project(project)
                            .build()
            );
        }

        // 9. 자식 스톤 생성
        Stone childStone = stoneRepository.save(
                Stone.builder()
                        .stoneName(dto.getStoneName())
                        .startTime(dto.getStartTime())
                        .endTime(dto.getEndTime())
                        .project(project)
                        .stoneManager(participant)
                        .chatCreation(dto.getChatCreation() != null ? dto.getChatCreation() : false)
                        .parentStoneId(parentStone.getId())
                        .taskCreation(true)
                        .milestone(BigDecimal.ZERO)
                        .status(StoneStatus.PROGRESS)
                        .build()
        );

        // 10. 상위 스톤의 자식 리스트 등록
        childStoneListRepository.save(
                ChildStoneList.builder()
                        .stone(parentStone)
                        .childStone(childStone)
                        .build()
        );

        // 11. 스톤 참여자 등록
        if (dto.getParticipantIds() != null && !dto.getParticipantIds().isEmpty()) {
            List<StoneParticipant> participantEntities = dto.getParticipantIds().stream()
                    .map(uuid -> {
                        WorkspaceParticipant wp = workspaceParticipantRepository
                                .findByWorkspaceIdAndUserId(workspace.getId(), uuid)
                                .orElseThrow(() -> new EntityNotFoundException("참여자 정보를 찾을 수 없습니다."));
                        return StoneParticipant.builder()
                                .stone(childStone)
                                .workspaceParticipant(wp)
                                .build();
                    }).toList();
            stoneParticipantRepository.saveAll(participantEntities);
        }

        // 12. 프로젝트/마일스톤 반영
        project.incrementStoneCount();
        projectRepository.save(project);
        milestoneCalculator.updateStoneAndParents(parentStone);

        return childStone.getId();
    }



// 스톤 참여자 추가 (전체 갱신 방식)
    public void joinStoneParticipant(String userId, StoneParticipantListDto dto) {

        // 1. 스톤 조회
        Stone stone = stoneRepository.findById(dto.getStoneId())
                .orElseThrow(() -> new EntityNotFoundException("스톤을 찾을 수 없습니다."));

        // 2. 프로젝트, 워크스페이스 조회
        Project project = stone.getProject();
        Workspace workspace = project.getWorkspace();

        // 3. 요청자 검증
        WorkspaceParticipant requester = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 4. 권한 검증
        if (!requester.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            if (!project.getWorkspaceParticipant().getId().equals(requester.getId())
                    && !stone.getStoneManager().getId().equals(requester.getId())) {
                throw new IllegalArgumentException("관리자나 프로젝트 담당자 혹은 스톤 담당자가 아닙니다.");
            }
        }

        // 5. 추가할 대상이 없으면 기존 참여자 모두 삭제
        if (dto.getStoneParticipantList() == null || dto.getStoneParticipantList().isEmpty()) {
            List<StoneParticipant> existingParticipants = stoneParticipantRepository.findAllByStone(stone);
            stoneParticipantRepository.deleteAll(existingParticipants);
            return;
        }

        // 6. 중복 자동 필터링용 Set 생성
        Set<UUID> newUserIds = new HashSet<>(dto.getStoneParticipantList());

        // 7. 기존 스톤 참여자 조회
        List<StoneParticipant> existingParticipants = stoneParticipantRepository.findAllByStone(stone);
        Set<UUID> existingUserIds = existingParticipants.stream()
                .map(sp -> sp.getWorkspaceParticipant().getUserId())
                .collect(Collectors.toSet());

        // 8. 삭제 대상 = 기존 - 신규
        Set<UUID> deleteTargetIds = existingUserIds.stream()
                .filter(id -> !newUserIds.contains(id))
                .collect(Collectors.toSet());

        if (!deleteTargetIds.isEmpty()) {
            List<StoneParticipant> toDelete = existingParticipants.stream()
                    .filter(sp -> deleteTargetIds.contains(sp.getWorkspaceParticipant().getUserId()))
                    .toList();

            // 스톤 참여자 삭제
            stoneParticipantRepository.deleteAll(toDelete);

            // 프로젝트 참여자에서도 제외 (다른 스톤에 참여 중이 아닌 경우에만)
            for (UUID deletedUserId : deleteTargetIds) {
                WorkspaceParticipant wp = workspaceParticipantRepository
                        .findByWorkspaceIdAndUserId(workspace.getId(), deletedUserId)
                        .orElse(null);

                if (wp != null) {
                    boolean stillInOtherStones = stoneParticipantRepository
                            .existsByWorkspaceParticipantAndStone_Project(wp, project);
                    if (!stillInOtherStones) {
                        projectParticipantRepository.deleteByProjectAndWorkspaceParticipant(project, wp);
                    }
                }
            }
        }

        // 9. 프로젝트 참여자 자동 추가 (중복 방지)
        for (UUID userUuid : newUserIds) {
            WorkspaceParticipant wp = workspaceParticipantRepository
                    .findByWorkspaceIdAndUserId(workspace.getId(), userUuid)
                    .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자를 찾을 수 없습니다."));

            boolean existsInProject = projectParticipantRepository.existsByProjectAndWorkspaceParticipant(project, wp);
            if (!existsInProject) {
                projectParticipantRepository.save(
                        ProjectParticipant.builder()
                                .project(project)
                                .workspaceParticipant(wp)
                                .build()
                );
            }
        }

        // 10. 스톤 참여자 신규 추가 (기존에 없던 경우만)
        List<StoneParticipant> newParticipants = new ArrayList<>();
        for (UUID userUuid : newUserIds) {
            WorkspaceParticipant wp = workspaceParticipantRepository
                    .findByWorkspaceIdAndUserId(workspace.getId(), userUuid)
                    .orElseThrow(() -> new EntityNotFoundException("참여자 정보를 찾을 수 없습니다."));

            boolean existsInStone = stoneParticipantRepository.existsByStoneAndWorkspaceParticipant(stone, wp);
            if (!existsInStone) {
                newParticipants.add(
                        StoneParticipant.builder()
                                .stone(stone)
                                .workspaceParticipant(wp)
                                .build()
                );
            }
        }

        if (!newParticipants.isEmpty()) {
            stoneParticipantRepository.saveAll(newParticipants);
        }
    }




// 스톤 참여자 리스트 삭제
    public void deleteStoneParticipantList(String userId, StoneParticipantListDto dto) {

        // 1. 스톤 조회
        Stone stone = stoneRepository.findById(dto.getStoneId())
                .orElseThrow(() -> new EntityNotFoundException("스톤을 찾을 수 없습니다."));

        // 2. 스톤이 포함된 프로젝트 조회
        Project project = stone.getProject();

        // 3. 요청 사용자 검증
        WorkspaceParticipant requester = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(project.getWorkspace().getId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 4. 권한 검증 (프로젝트 담당자 or 스톤 담당자 or 관리자)
        if (!requester.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            if (!project.getWorkspaceParticipant().getId().equals(requester.getId())
                    && !stone.getStoneManager().getId().equals(requester.getId())) {
                throw new IllegalArgumentException("프로젝트 담당자 혹은 스톤 담당자가 아닙니다.");
            }
        }

        // 5. 스톤 참여자 삭제 (UUID 기반으로 변경) // 추가
        if (dto.getStoneParticipantList() != null && !dto.getStoneParticipantList().isEmpty()) {
            List<StoneParticipant> toDeleteStoneParticipants = new ArrayList<>();

            for (UUID userUuid : dto.getStoneParticipantList()) { // String → UUID 변경 // 추가
                WorkspaceParticipant wp = workspaceParticipantRepository
                        .findByWorkspaceIdAndUserId(project.getWorkspace().getId(), userUuid) // 추가
                        .orElseThrow(() -> new EntityNotFoundException("참여자 정보를 찾을 수 없습니다."));

                StoneParticipant stoneParticipant = stoneParticipantRepository
                        .findByStoneAndWorkspaceParticipant(stone, wp)
                        .orElseThrow(() -> new EntityNotFoundException("스톤 참여자 정보를 찾을 수 없습니다."));

                toDeleteStoneParticipants.add(stoneParticipant);

                ProjectParticipant projectParticipant = projectParticipantRepository
                        .findByProjectAndWorkspaceParticipant(project, wp)
                        .orElse(null);

                if (projectParticipant != null) {
                    projectParticipantRepository.delete(projectParticipant);
                }
            }

            stoneParticipantRepository.deleteAll(toDeleteStoneParticipants);
        }
    }


// 스톤 참여자 전체 삭제
    public void deleteAllStoneParticipants(String userId, String stoneId) {

        // 1. 스톤 조회
        Stone stone = stoneRepository.findById(stoneId)
                .orElseThrow(() -> new EntityNotFoundException("스톤을 찾을 수 없습니다."));

        // 2. 스톤이 포함된 프로젝트 조회
        Project project = stone.getProject();

        // 3. 요청 사용자 검증
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(project.getWorkspace().getId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 4. 권한 검증 (프로젝트 담당자 or 스톤 담당자만 가능)
        if (!participant.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            if (!project.getWorkspaceParticipant().getId().equals(participant.getId())
                    && !stone.getStoneManager().getId().equals(participant.getId())) {
                throw new IllegalArgumentException("관리자이거나 프로젝트 담당자 혹은 스톤 담당자가 아닙니다.");
            }
        }

        // 5. 스톤 참여자 전체 삭제
        List<StoneParticipant> participants = stoneParticipantRepository.findAllByStone(stone);
        if (!participants.isEmpty()) {
            stoneParticipantRepository.deleteAll(participants);
        }

        // 프로젝트 참여자 일괄 삭제
        List<ProjectParticipant> projectParticipants =
                projectParticipantRepository.findAllByProject(project);
        if (!projectParticipants.isEmpty()) {
            projectParticipantRepository.deleteAll(projectParticipants);
        }
    }


// 스톤 보임/안보임 설정(프로젝트 캘린더 조회용 API)
    public void settingStone(String userId, StoneSettingDto dto) {

        // 1. 스톤 조회
        Stone stone = stoneRepository.findById(dto.getStoneId())
                .orElseThrow(()->new EntityNotFoundException("스톤을 찾을 수 없습니다."));

        // 2. 스톤이 포함된 프로젝트 객체 생성
        Project project = stone.getProject();

        // 3. 참여자 검증
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(project.getWorkspace().getId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 4. 스톤 참여자 조회
        StoneParticipant stoneParticipant = stoneParticipantRepository
                .findByStoneAndWorkspaceParticipant(stone, participant)
                .orElseThrow(() -> new EntityNotFoundException("스톤참여자 정보가 없습니다."));

        // 5. isMilestoneHidden 값 설정
        stoneParticipant.updateMilestoneHidden(dto.getIsMilestoneHidden());

        // 6. 변경사항 저장
        stoneParticipantRepository.save(stoneParticipant);
    }

    // 스톤 정보 수정
    public String modifyStone(String userId, StoneModifyDto dto) {
        // 1. 스톤 조회
        Stone stone = stoneRepository.findById(dto.getStoneId())
                .orElseThrow(() -> new EntityNotFoundException("스톤을 찾을 수 없습니다."));

        // 2. 스톤이 속한 프로젝트 및 워크스페이스 조회
        Project project = stone.getProject();
        Workspace workspace = project.getWorkspace();

        // 3. 요청 사용자 검증
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 4. 권한 검증
        if (!participant.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            if (!project.getWorkspaceParticipant().getId().equals(participant.getId())
                    && !stone.getStoneManager().getId().equals(participant.getId())) {
                throw new IllegalArgumentException("관리자이거나 프로젝트 담당자 혹은 스톤 담당자가 아닙니다.");
            }
        }

        // 5. 스톤 기간 검증 (프로젝트 기간 내에만 수정 가능)
        if (dto.getStartTime().isBefore(project.getStartTime()) ||
                dto.getEndTime().isAfter(project.getEndTime())) {
            throw new IllegalArgumentException("프로젝트 기간 내에만 스톤 수정이 가능합니다.");
        }

        // 6. 기본 필드 수정 (null 체크해서 들어온 값만 반영)
        if (dto.getStoneName() != null) stone.setStoneName(dto.getStoneName());
        if (dto.getStartTime() != null) stone.setStartTime(dto.getStartTime());
        if (dto.getEndTime() != null) stone.setEndTime(dto.getEndTime());

        // 7. 채팅방 생성 여부 방어 로직
        if (dto.getChatCreation() != null) {
            boolean prev = stone.getChatCreation();  // 현재 DB에 저장된 상태
            boolean next = dto.getChatCreation();   // 수정 요청 값

            // 이미 true인데 false로 바꾸려 하면 막기
            if (prev && !next) {
                throw new IllegalStateException("이미 생성된 채팅방은 비활성화할 수 없습니다.");
            }

//            // false → true 전환만 허용
//            if (!prev && next) {
//                stone.setChatCreation(true);
//                // todo 추후에 여기서 chatRoomService.createChatRoom(stone) 붙여야함
//            }
        }
        if (dto.getEndTime() != null) {
            stone.setEndTime(dto.getEndTime());
        }

        // 8. 수정된 스톤 저장
        stoneRepository.save(stone);
        milestoneCalculator.updateStoneAndParents(stone);
        return stone.getId();
    }

    // 스톤 담당자 수정
    public void modifyStoneManager(String userId, StoneManagerModifyDto dto) {

        // 1. 스톤 조회
        Stone stone = stoneRepository.findById(dto.getStoneId())
                .orElseThrow(() -> new EntityNotFoundException("스톤을 찾을 수 없습니다."));

        // 2. 스톤이 포함된 프로젝트 및 워크스페이스 조회
        Project project = stone.getProject();
        Workspace workspace = project.getWorkspace();

        // 3. 요청 사용자 검증
        WorkspaceParticipant requester = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 4. 권한 검증 (관리자이거나 프로젝트 담당자 또는 기존 스톤 담당자만 가능)
        if (!requester.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            if (!project.getWorkspaceParticipant().getId().equals(requester.getId())
                    && !stone.getStoneManager().getId().equals(requester.getId())) {
                throw new IllegalArgumentException("관리자나 프로젝트 담당자 혹은 스톤 담당자가 아닙니다.");
            }
        }

        // 5. 새 담당자 검증
        WorkspaceParticipant newManager = workspaceParticipantRepository.findByWorkspaceIdAndUserId(workspace.getId(),dto.getNewManagerUserId())
                .orElseThrow(() -> new EntityNotFoundException("새 담당자 정보를 찾을 수 없습니다."));

        // 같은 워크스페이스 소속인지 검증 (보안 강화)
        if (!newManager.getWorkspace().getId().equals(workspace.getId())) {
            throw new IllegalArgumentException("해당 담당자는 같은 워크스페이스 소속이 아닙니다.");
        }

        // 6. 스톤 담당자 교체
        stone.setStoneManager(newManager);

        // 7. (선택) 새 담당자가 프로젝트 참여자가 아니라면 자동 등록
        boolean existsInProject = projectParticipantRepository.existsByProjectAndWorkspaceParticipant(project, newManager);
        if (!existsInProject) {
            ProjectParticipant newProjectParticipant = ProjectParticipant.builder()
                    .project(project)
                    .workspaceParticipant(newManager)
                    .build();
            projectParticipantRepository.save(newProjectParticipant);
        }

        // 8. 변경된 스톤 저장
        stoneRepository.save(stone);

        //todo chatCreation이 true면 채팅방 생성 로직 추가해야함. 수정의 경우, false -> true가 돼도 기존에 채팅방이 생성되어있으면 생성안되게

    }

    // 스톤 삭제
    public void deleteStone(String userId, String stoneId) {

        // 1. 스톤 조회
        Stone stone = stoneRepository.findById(stoneId)
                .orElseThrow(() -> new EntityNotFoundException("스톤을 찾을 수 없습니다."));

        // 2. 부모 스톤이 없는 경우 (최상위 스톤) 삭제 불가
        if (stone.getParentStoneId() == null) {
            throw new IllegalArgumentException("최상위 스톤은 삭제할 수 없습니다. (프로젝트 루트 스톤)");
        }

        // 3. 스톤이 포함된 프로젝트 및 워크스페이스 조회
        Project project = stone.getProject();
        Workspace workspace = project.getWorkspace();

        // 4. 요청 사용자 검증
        WorkspaceParticipant requester = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 5. 권한 검증 (ADMIN, 프로젝트 담당자, 스톤 담당자)
        if (requester.getWorkspaceRole() != WorkspaceRole.ADMIN &&
                !project.getWorkspaceParticipant().getId().equals(requester.getId()) &&
                !stone.getStoneManager().getId().equals(requester.getId())) {
            throw new IllegalArgumentException("관리자, 프로젝트 담당자, 혹은 스톤 담당자만 삭제 가능합니다.");
        }

        // 6. 이미 삭제된 스톤인지 확인
        if (Boolean.TRUE.equals(stone.getIsDelete())) {
            throw new IllegalStateException("이미 삭제된 스톤입니다.");
        }

        // 7. 프로젝트 마일스톤 반영
        // 완료된 스톤이었다면 완료 카운트 감소
        if (stone.getStatus() == StoneStatus.COMPLETED) {
            project.decrementCompletedCount();
        }
        project.decrementStoneCount();
        projectRepository.save(project);

        // 8. 스톤 논리 삭제
        stone.setIsDelete(true);
        stoneRepository.save(stone);

        // 부모 스톤 마일스톤 재계산
        if (stone.getParentStoneId() != null) {
            stoneRepository.findById(stone.getParentStoneId())
                    .ifPresent(milestoneCalculator::updateStoneAndParents);
        }

        // 9. 스톤 참여자 하드 삭제
        List<StoneParticipant> stoneParticipants = stoneParticipantRepository.findAllByStone(stone);
        if (!stoneParticipants.isEmpty()) {
            stoneParticipantRepository.deleteAll(stoneParticipants);
        }

        // 10. 프로젝트 참여자 조건부 삭제
        for (StoneParticipant sp : stoneParticipants) {
            WorkspaceParticipant wp = sp.getWorkspaceParticipant();

            // 해당 참여자가 이 프로젝트의 다른 스톤에도 속해 있는지 확인
            boolean stillExists = stoneParticipantRepository.existsByStone_ProjectAndWorkspaceParticipant(project, wp);

            // 다른 스톤에도 없으면 프로젝트 참여자에서도 제거
            if (!stillExists) {
                ProjectParticipant projectParticipant = projectParticipantRepository
                        .findByProjectAndWorkspaceParticipant(project, wp)
                        .orElse(null);

                if (projectParticipant != null) {
                    projectParticipantRepository.delete(projectParticipant);
                }
            }
        }

        // 11. 변경 저장
        stoneRepository.save(stone);
    }

    // 스톤 완료 처리
    public void completeStone(String userId, String stoneId) {
        Stone stone = stoneRepository.findById(stoneId)
                .orElseThrow(() -> new EntityNotFoundException("스톤을 찾을 수 없습니다."));

        if (stone.getParentStoneId() == null) {
            throw new IllegalArgumentException("최상위 스톤은 완료 처리할 수 없습니다.");
        }

        Project project = stone.getProject();
        Workspace workspace = project.getWorkspace();

        WorkspaceParticipant requester = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        if (requester.getWorkspaceRole() != WorkspaceRole.ADMIN &&
                !project.getWorkspaceParticipant().getId().equals(requester.getId()) &&
                !stone.getStoneManager().getId().equals(requester.getId())) {
            throw new IllegalArgumentException("완료 권한이 없습니다.");
        }

        if (stone.getStatus() == StoneStatus.COMPLETED) {
            throw new IllegalStateException("이미 완료된 스톤입니다.");
        }

        // 모든 태스크 완료 여부 확인
        List<Task> tasks = taskRepository.findAllByStone(stone);
        boolean allTasksCompleted = tasks.stream().allMatch(Task::getIsDone);
        if (!allTasksCompleted) {
            throw new IllegalStateException("모든 태스크가 완료되어야 스톤을 완료 처리할 수 있습니다.");
        }

        // 완료 처리
        stone.setStatus(StoneStatus.COMPLETED);
        stone.setMilestone(BigDecimal.valueOf(100));
        stoneRepository.save(stone);

        // 부모 스톤 갱신
        if (stone.getParentStoneId() != null) {
            stoneRepository.findById(stone.getParentStoneId())
                    .ifPresent(milestoneCalculator::updateStoneAndParents);
        }
    }



    // 프로젝트 별 나의 마일스톤 조회(isDelete = true 제외, stoneStatus Completed 제외)
    public List<ProjectMilestoneResDto> milestoneList(String userId, String workspaceId) {

        // 1. 워크스페이스, 사용자 검증
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스가 존재하지 않습니다."));

        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspaceId, UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 존재하지 않습니다."));

        // 2. 내가 속한 프로젝트 + 스톤 fetch join으로 조회
        List<ProjectParticipant> projectParticipants =
                projectParticipantRepository.findAllWithStonesByWorkspaceParticipant(participant);

        // 프로젝트 중복 제거
        List<Project> uniqueProjects = projectParticipants.stream()
                .map(ProjectParticipant::getProject)
                .distinct()
                .toList();

        // 3. 내가 참여 중인 스톤들을 미리 캐싱 (성능 최적화용)
        List<StoneParticipant> activeStoneParticipants =
                stoneParticipantRepository.findAllActiveWithStoneByWorkspaceParticipant(participant);


        // 3. DTO 변환
        // 리턴용 리스트 생성
        List<ProjectMilestoneResDto> result = new ArrayList<>();

        for (Project project : uniqueProjects) {
            List<MilestoneResDto> milestoneDtos = activeStoneParticipants.stream()
                    .filter(sp -> sp.getStone().getProject().equals(project))
                    .filter(sp -> !sp.getIsMilestoneHidden())
                    .map(sp -> MilestoneResDto.fromEntity(sp.getStone()))
                    .toList();

            result.add(ProjectMilestoneResDto.builder()
                    .projectId(project.getId())
                    .projectName(project.getProjectName())
                    .milestoneResDtoList(milestoneDtos)
                    .build());
        }

        return result;
    }


    // 스톤 상세 정보 조회
    public StoneDetailResDto getStoneDetail(String userId, String stoneId) {

        // 1. 스톤 조회
        Stone stone = stoneRepository.findById(stoneId)
                .orElseThrow(() -> new EntityNotFoundException("스톤을 찾을 수 없습니다."));

        // 2. 스톤이 속한 프로젝트 및 워크스페이스 조회
        Project project = stone.getProject();
        Workspace workspace = project.getWorkspace();

        // 3. 요청 사용자 검증 (워크스페이스 소속 여부)
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 4. 접근 권한 검증 (ADMIN, 프로젝트 담당자, 스톤 담당자, 스톤 참여자)
        boolean isAuthorized =
                participant.getWorkspaceRole().equals(WorkspaceRole.ADMIN) ||
                        project.getWorkspaceParticipant().getId().equals(participant.getId()) ||
                        stone.getStoneManager().getId().equals(participant.getId()) ||
                        stoneParticipantRepository.existsByStoneAndWorkspaceParticipant(stone, participant);

        if (!isAuthorized) {
            throw new IllegalArgumentException("해당 스톤에 접근할 권한이 없습니다.");
        }

        // 5. 스톤이 삭제된 경우 예외
        if (Boolean.TRUE.equals(stone.getIsDelete())) {
            throw new IllegalStateException("삭제된 스톤입니다.");
        }

        // 6. 태스크 목록 조회 및 변환
        List<Task> tasks = taskRepository.findAllByStone(stone);
        List<TaskResDto> taskResDtoList = tasks.stream()
                .map(TaskResDto::fromEntity)
                .toList();

        // 7. 삭제되지 않은 스톤 참여자만 조회
        List<StoneParticipant> stoneParticipants =
                stoneParticipantRepository.findAllByStoneAndWorkspaceParticipant_IsDeleteFalse(stone);

        // 8. DTO 변환
        List<StoneParticipantDto> stoneParticipantDtoList = stoneParticipants.stream()
                .map(sp -> StoneParticipantDto.builder()
                        .participantId(sp.getWorkspaceParticipant().getId())
                        .participantName(sp.getWorkspaceParticipant().getUserName())
                        .userId(sp.getWorkspaceParticipant().getUserId())
                        .build()
                )
                .toList();

        // 9. DTO 조립 및 반환
        return StoneDetailResDto.fromEntity(stone, taskResDtoList, stoneParticipantDtoList);
    }


    // 스톤 참여자 목록 조회
    @Transactional(readOnly = true)
    public List<StoneParticipantDto> getStoneParticipantList(String userId, String stoneId) {

        // 1. 스톤 조회
        Stone stone = stoneRepository.findById(stoneId)
                .orElseThrow(() -> new EntityNotFoundException("스톤을 찾을 수 없습니다."));

        // 2. 스톤이 속한 프로젝트 및 워크스페이스 조회
        Project project = stone.getProject();
        Workspace workspace = project.getWorkspace();

        // 3. 요청 사용자 검증
        WorkspaceParticipant requester = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 4. 접근 권한 검증
        boolean isAuthorized =
                requester.getWorkspaceRole().equals(WorkspaceRole.ADMIN) ||
                        project.getWorkspaceParticipant().getId().equals(requester.getId()) ||
                        stone.getStoneManager().getId().equals(requester.getId()) ||
                        stoneParticipantRepository.existsByStoneAndWorkspaceParticipant(stone, requester);

        if (!isAuthorized) {
            throw new IllegalArgumentException("해당 스톤에 접근할 권한이 없습니다.");
        }

        // 5. 스톤 참여자 목록 조회 (워크스페이스에서 삭제되지 않은 인원만)
        List<StoneParticipant> participants =
                stoneParticipantRepository.findAllByStoneAndWorkspaceParticipant_IsDeleteFalse(stone);

        if (participants.isEmpty()) {
            return Collections.emptyList();
        }

        // 6. userId(UUID) 리스트 수집
        List<UUID> userIdList = participants.stream()
                .map(sp -> sp.getWorkspaceParticipant().getUserId())
                .distinct() // 중복 제거용
                .toList();

        // 7. user-service에서 이메일 등 상세 정보 조회 (Feign)
        UserIdListDto userIdListDto = new UserIdListDto(userIdList);
        UserInfoListResDto userInfoListResDto = userFeign.fetchUserListInfo(userIdListDto);

        // 8. 결과 매핑 (UUID → UserInfoResDto)
        Map<UUID, UserInfoResDto> userInfoMap = userInfoListResDto.getUserInfoList().stream()
                .collect(Collectors.toMap(UserInfoResDto::getUserId, u -> u));

        // 9. DTO 조립
        return participants.stream()
                .map(sp -> {
                    WorkspaceParticipant wp = sp.getWorkspaceParticipant();
                    UserInfoResDto info = userInfoMap.get(wp.getUserId());

                    return StoneParticipantDto.builder()
                            .participantId(wp.getId())
                            .participantName(wp.getUserName())
                            .userId(wp.getUserId())
                            .userEmail(info != null ? info.getUserEmail() : null)
                            .build();
                })
                .toList();
    }


    // 공통 메서드 : 부모가 최상위 스톤인지 파악하는 메서드
    public Boolean findTopStone(Stone stone) {

        if (stone.getParentStoneId() == null) {
            return false;
        }

        Optional<Stone> parentOpt = stoneRepository.findById(stone.getParentStoneId());

        if (parentOpt.isEmpty()) {
            return false;
        }

        Stone parent = parentOpt.get();

        return parent.getParentStoneId() == null;
    }

}
