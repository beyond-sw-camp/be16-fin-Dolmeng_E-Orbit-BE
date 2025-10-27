package com.Dolmeng_E.workspace.domain.stone.service;

import com.Dolmeng_E.workspace.common.service.AccessCheckService;
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

    }

    // 일반 스톤 생성
    public String createStone(String userId, StoneCreateDto dto) {

        // 1. 상위 스톤 조회
        Stone parentStone = stoneRepository.findById(dto.getParentStoneId())
                .orElseThrow(() -> new EntityNotFoundException("상위스톤이 존재하지 않습니다."));

        // 2. 상위 스톤을 통해 프로젝트, 워크스페이스 추적
        Project project = parentStone.getProject();
        Workspace workspace = project.getWorkspace();

        // 3. 현재 요청 사용자가 워크스페이스에 속해 있는지 검증
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 4. 권한 검증 (프로젝트 담당자이거나, 스톤 생성 권한이 있어야 함, 혹은 관리자)
        if (!participant.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            if (!project.getWorkspaceParticipant().getId().equals(participant.getId())) {
                accessCheckService.validateAccess(participant, "ws_acc_list_2");
            }
        }

        // 5. 스톤 참여자들 중 프로젝트 참여자에 아직 등록되지 않은 경우 자동 등록
        if (dto.getParticipantIds() != null && !dto.getParticipantIds().isEmpty()) {
            for (String participantId : dto.getParticipantIds()) {
                WorkspaceParticipant wp = workspaceParticipantRepository.findById(participantId)
                        .orElseThrow(() -> new EntityNotFoundException("참여자 정보를 찾을 수 없습니다."));

                // 요청자 자신은 중복 추가하지 않음
                if (wp.getId().equals(participant.getId())) continue;

                boolean exists = projectParticipantRepository.existsByProjectAndWorkspaceParticipant(project, wp);
                if (!exists) {
                    ProjectParticipant newProjectParticipant = ProjectParticipant.builder()
                            .project(project)
                            .workspaceParticipant(wp)
                            .build();
                    projectParticipantRepository.save(newProjectParticipant);
                }
            }
        }

        // 6. 스톤 담당자 프로젝트 참여자 등록 (중복 방지 로직 수정)
        boolean alreadyProjectParticipant = projectParticipantRepository.existsByProjectAndWorkspaceParticipant(project, participant);
        if (!alreadyProjectParticipant) {
            projectParticipantRepository.save(
                    ProjectParticipant.builder()
                            .workspaceParticipant(participant)
                            .project(project)
                            .build()
            );
        }

        // 7. 자식 스톤 생성
        Stone childStone = stoneRepository.save(
                Stone.builder()
                        .stoneName(dto.getStoneName())
                        .startTime(dto.getStartTime())
                        .endTime(dto.getEndTime())
                        .project(project)
                        .stoneManager(participant) // 스톤 담당자
                        .chatCreation(dto.getChatCreation() != null ? dto.getChatCreation() : false)
                        .parentStoneId(parentStone.getId()) // 상위 스톤 참조
                        .taskCreation(true) // 기본값 true
                        .milestone(dto.getMilestone() != null ? dto.getMilestone() : BigDecimal.ZERO)
                        .status(StoneStatus.PROGRESS)
                        .build()
        );

        // 8. 상위 스톤의 자식 스톤 리스트 등록
        childStoneListRepository.save(
                ChildStoneList.builder()
                        .stone(parentStone)
                        .childStone(childStone)
                        .build()
        );

        // 9. 스톤 참여자 등록
        if (dto.getParticipantIds() != null && !dto.getParticipantIds().isEmpty()) {
            List<StoneParticipant> participantEntities = new ArrayList<>();

            for (String participantId : dto.getParticipantIds()) {
                WorkspaceParticipant stoneParticipant = workspaceParticipantRepository.findById(participantId)
                        .orElseThrow(() -> new EntityNotFoundException("참여자 정보를 찾을 수 없습니다."));

                StoneParticipant entity = StoneParticipant.builder()
                        .stone(childStone)
                        .workspaceParticipant(stoneParticipant)
                        .build();

                participantEntities.add(entity);
            }

            stoneParticipantRepository.saveAll(participantEntities);
        }

        // 10. 프로젝트 마일스톤 반영
        project.incrementStoneCount();
        projectRepository.save(project);

        return childStone.getId();
    }


// 스톤 참여자 추가
    public void joinStoneParticipant(String userId, StoneParticipantListDto dto) {

        // 1. 스톤 조회
        Stone stone = stoneRepository.findById(dto.getStoneId())
                .orElseThrow(()->new EntityNotFoundException("스톤을 찾을 수 없습니다."));

        // 2. 스톤이 포함된 프로젝트 객체 생성
        Project project = stone.getProject();

        // 3. 참여자 검증
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(project.getWorkspace().getId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 4. 스톤 관련 권한 검증(프로젝트 담당자와 스톤 담당자만 참여자 추가 가능하도록 혹은 관리자)
        if (!participant.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            if (!project.getWorkspaceParticipant().getId().equals(participant.getId())
                    && !stone.getStoneManager().getId().equals(participant.getId())) {
                throw new IllegalArgumentException("관리자나 프로젝트 담당자 혹은 스톤 담당자가 아닙니다.");
            }
        }

        // 5. 중복참여자 검증
        List<String> duplicateNames = new ArrayList<>();

        if (dto.getStoneParticipantList() != null && !dto.getStoneParticipantList().isEmpty()) {
            for (String wsPtId : dto.getStoneParticipantList()) {
                WorkspaceParticipant wsPt = workspaceParticipantRepository.findById(wsPtId)
                        .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자를 찾을 수 없습니다."));

                if (stoneParticipantRepository.existsByStoneAndWorkspaceParticipant(stone, wsPt)) {
                    duplicateNames.add(wsPt.getUserName());
                }
            }

            if (!duplicateNames.isEmpty()) {
                throw new IllegalArgumentException(String.join(", ", duplicateNames) + "은/는 이미 존재하는 참여자입니다.");
            }
        }

        // 6. 프로젝트 참여자에 추가
        if (dto.getStoneParticipantList() != null && !dto.getStoneParticipantList().isEmpty()) {
            for (String wpId : dto.getStoneParticipantList()) {

                // 워크스페이스 참여자 조회
                WorkspaceParticipant wp = workspaceParticipantRepository.findById(wpId)
                        .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자를 찾을 수 없습니다."));

                // 이미 프로젝트에 포함되어 있는지 확인
                boolean exists = projectParticipantRepository.existsByProjectAndWorkspaceParticipant(project, wp);

                // 포함되어 있지 않으면 새로 추가
                if (!exists) {
                    ProjectParticipant newProjectParticipant = ProjectParticipant.builder()
                            .project(project)
                            .workspaceParticipant(wp)
                            .build();
                    projectParticipantRepository.save(newProjectParticipant);
                }
            }
        }

        // 7. 기존 참여자 조회
        List<StoneParticipant> existingParticipants = stoneParticipantRepository.findAllByStone(stone);
        // true일 경우 기존값 존재, false의 경우 기존값 없다는 뜻
        boolean hasExisting = !existingParticipants.isEmpty();

        // 8. 스톤에 참여자가 없거나, 중복되지 않은 경우 새로 저장
        if (dto.getStoneParticipantList() != null && !dto.getStoneParticipantList().isEmpty()) {
            // dto에 넣어놓은 id 리스트
            Set<String> participantIds = dto.getStoneParticipantList();
            // 엔티티에 추가할 리스트 생성
            List<StoneParticipant> newParticipants = new ArrayList<>();

            // dto의 각 id별로 워크스페이스 참여자 조회
            for (String id : participantIds) {
                WorkspaceParticipant wp = workspaceParticipantRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("참여자 정보를 찾을 수 없습니다."));

                // 기존 참여자가 없거나, 해당 참여자가 DB에 없는 경우에만 추가(중복 추가 검증)
                if (!hasExisting || !stoneParticipantRepository.existsByStoneAndWorkspaceParticipant(stone, wp)) {
                    // 새로운 스톤 참여자 엔티티 생성 및 리스트에 추가
                    newParticipants.add(
                            StoneParticipant.builder()
                                    .stone(stone)
                                    .workspaceParticipant(wp)
                                    .build()
                    );
                }
            }

            // 모두 저장(새로 구성한 리스트가 비어있지 않다면)
            if (!newParticipants.isEmpty()) {
                // DB에 일괄 저장 (saveAll()로 bulk insert)
                stoneParticipantRepository.saveAll(newParticipants);
            }
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
        // 5. 스톤 참여자 삭제 (일괄 처리)
        if (dto.getStoneParticipantList() != null && !dto.getStoneParticipantList().isEmpty()) {

            List<StoneParticipant> toDeleteStoneParticipants = new ArrayList<>();

            for (String participantId : dto.getStoneParticipantList()) {
                WorkspaceParticipant wp = workspaceParticipantRepository.findById(participantId)
                        .orElseThrow(() -> new EntityNotFoundException("참여자 정보를 찾을 수 없습니다."));

                StoneParticipant stoneParticipant = stoneParticipantRepository
                        .findByStoneAndWorkspaceParticipant(stone, wp)
                        .orElseThrow(() -> new EntityNotFoundException("스톤 참여자 정보를 찾을 수 없습니다."));

                toDeleteStoneParticipants.add(stoneParticipant);

                // 프로젝트 참여자 삭제
                ProjectParticipant projectParticipant = projectParticipantRepository
                        .findByProjectAndWorkspaceParticipant(project, wp)
                        .orElse(null); // 아래에서 검증 후 삭제하기 위해 orElse 사용..

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
    public void modifyStone(String userId, StoneModifyDto dto) {
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

        // 4. 스톤 관련 권한 검증(프로젝트 담당자와 스톤 담당자만 참여자 추가 가능하도록)
        if (!participant.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            if (!project.getWorkspaceParticipant().getId().equals(participant.getId())
                    && !stone.getStoneManager().getId().equals(participant.getId())) {
                throw new IllegalArgumentException("관리자이거나 프로젝트 담당자 혹은 스톤 담당자가 아닙니다.");
            }
        }

        // 5. 기본 필드 수정 (null 체크해서 들어온 값만 반영)
        if (dto.getStoneName() != null) {
            stone.setStoneName(dto.getStoneName());
        }
        if (dto.getStartTime() != null) {
            stone.setStartTime(dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            stone.setEndTime(dto.getEndTime());
        }

        // 6. 수정된 스톤 저장
        stoneRepository.save(stone);
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
        WorkspaceParticipant newManager = workspaceParticipantRepository.findById(dto.getNewManagerId())
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
        // 1. 스톤 조회
        Stone stone = stoneRepository.findById(stoneId)
                .orElseThrow(() -> new EntityNotFoundException("스톤을 찾을 수 없습니다."));

        // 2. 부모 스톤이 없는 경우 (최상위 스톤) 삭제 불가
        if (stone.getParentStoneId() == null) {
            throw new IllegalArgumentException("최상위 스톤은 완료 처리할 수 없습니다. (프로젝트 루트 스톤)");
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
            throw new IllegalArgumentException("관리자, 프로젝트 담당자, 혹은 스톤 담당자만 완료처리 가능합니다.");
        }

        // 6. 완료처리
        if (stone.getStatus() == StoneStatus.COMPLETED) {
            throw new IllegalStateException("이미 완료된 스톤입니다.");
        }
        // 7. 스톤 완료 처리 및 마일스톤 100%
        stone.setStatus(StoneStatus.COMPLETED);
        stone.setMilestone(BigDecimal.valueOf(100.0));

        // 8. 부모가 최상위 스톤인지 확인
        Boolean isParentTop = findTopStone(stone);

        if (isParentTop) {
            // 부모가 최상위 스톤이면 프로젝트 진행률 갱신
            project.incrementCompletedCount();
            projectRepository.save(project);
        }

        // 9. 스톤 저장
        stoneRepository.save(stone);
    }

// 프로젝트 별 내 마일스톤 조회(isDelete = true 제외, stoneStatus Completed 제외)
    public List<ProjectMilestoneResDto> milestoneList(String userId, String workspaceId) {

        // 1. 워크스페이스, 사용자 검증
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스가 존재하지 않습니다."));

        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspaceId, UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 존재하지 않습니다."));

        // 2. fetch join으로 프로젝트 + 스톤을 한 번에 로드
        // 이전 구조는 for문을 통해 객체를 구해서 N + 1 이슈가 있었습니다.
        List<ProjectParticipant> projectParticipants =
                // 내가 속한 프로젝트와 그 안의 스톤들을 조회하는 쿼리문
                projectParticipantRepository.findAllWithStonesByWorkspaceParticipant(participant);

        // 3. DTO 변환

        // 리턴용 리스트 생성
        List<ProjectMilestoneResDto> result = new ArrayList<>();

        for (ProjectParticipant pp : projectParticipants) {
            Project project = pp.getProject();

            // 프로젝트의 스톤 리스트 가져오기 (fetch join으로 이미 로드됨)
            List<Stone> stones = project.getStones();

            List<StoneParticipant> activeStoneParticipants =
                    //내가 참여 중인 스톤들만 미리 캐싱하는 쿼리문
                    stoneParticipantRepository.findAllActiveWithStoneByWorkspaceParticipant(participant);

            List<MilestoneResDto> milestoneDtos = activeStoneParticipants.stream()
                    .filter(sp -> !sp.getIsMilestoneHidden())
                    .map(sp -> MilestoneResDto.fromEntity(sp.getStone()))
                    .toList();

            // 프로젝트별 마일스톤 응답 DTO 조립
            ProjectMilestoneResDto dto = ProjectMilestoneResDto.builder()
                    .projectId(project.getId())
                    .projectName(project.getProjectName())
                    .milestoneResDtoList(milestoneDtos)
                    .build();

            result.add(dto);
        }

        return result;
    }


// 스톤 상세 정보 조회

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

        // 6. 태스크 목록 조회
        List<Task> tasks = taskRepository.findAllByStone(stone);

        // 7. DTO 변환
        List<TaskResDto> taskResDtoList = tasks.stream()
                .map(TaskResDto::fromEntity)
                .toList();

        // 8. 스톤 상세 DTO 조립 후 리턴
        return StoneDetailResDto.fromEntity(stone, taskResDtoList);
    }




    // 스톤 참여자 목록 조회

    //ToDo: 다 하면 프로젝트 쪽 로직 완성

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
