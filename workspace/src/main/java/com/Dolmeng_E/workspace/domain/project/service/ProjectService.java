package com.Dolmeng_E.workspace.domain.project.service;

import com.Dolmeng_E.workspace.common.controller.DriveServiceClient;
import com.Dolmeng_E.workspace.common.controller.SearchServiceClient;
import com.Dolmeng_E.workspace.common.dto.ElementCountAndSizeResDto;
import com.Dolmeng_E.workspace.common.dto.UserIdListDto;
import com.Dolmeng_E.workspace.common.dto.UserInfoResDto;
import com.Dolmeng_E.workspace.common.service.AccessCheckService;
import com.Dolmeng_E.workspace.common.service.UserFeign;
import com.Dolmeng_E.workspace.domain.project.dto.*;
import com.Dolmeng_E.workspace.domain.project.entity.Project;
import com.Dolmeng_E.workspace.domain.project.entity.ProjectParticipant;
import com.Dolmeng_E.workspace.domain.project.entity.ProjectStatus;
import com.Dolmeng_E.workspace.domain.stone.dto.ProjectMemberOverviewDto;
import com.Dolmeng_E.workspace.domain.stone.dto.ProjectPeopleOverviewResDto;
import com.Dolmeng_E.workspace.domain.stone.dto.SimpleStoneRefDto;
import com.Dolmeng_E.workspace.domain.stone.entity.Stone;
import com.Dolmeng_E.workspace.domain.stone.entity.StoneParticipant;
import com.Dolmeng_E.workspace.domain.stone.entity.StoneStatus;
import com.Dolmeng_E.workspace.domain.stone.repository.StoneParticipantRepository;
import com.Dolmeng_E.workspace.domain.project.repository.ProjectParticipantRepository;
import com.Dolmeng_E.workspace.domain.project.repository.ProjectRepository;
import com.Dolmeng_E.workspace.domain.stone.dto.TopStoneCreateDto;
import com.Dolmeng_E.workspace.domain.stone.repository.StoneRepository;
import com.Dolmeng_E.workspace.domain.stone.service.StoneService;
import com.Dolmeng_E.workspace.domain.task.entity.Task;
import com.Dolmeng_E.workspace.domain.task.repository.TaskRepository;
import com.Dolmeng_E.workspace.domain.workspace.dto.DriveKafkaReqDto;
import com.Dolmeng_E.workspace.domain.workspace.entity.Workspace;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceRole;
import com.Dolmeng_E.workspace.domain.workspace.repository.WorkspaceParticipantRepository;
import com.Dolmeng_E.workspace.domain.workspace.repository.WorkspaceRepository;
import com.example.modulecommon.dto.CommonSuccessDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceRole.ADMIN;
import static redis.clients.jedis.search.aggr.Reducers.stddev;

@Service
@Transactional
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectParticipantRepository projectParticipantRepository;
    private final WorkspaceParticipantRepository workspaceParticipantRepository;
    private final WorkspaceRepository workspaceRepository;
    private final AccessCheckService accessCheckService;
    private final StoneService stoneService;
    private final StoneRepository stoneRepository;
    private final StoneParticipantRepository stoneParticipantRepository;
    private final TaskRepository taskRepository;
    private final UserFeign userFeign;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final DriveServiceClient driveServiceClient;
    private final SearchServiceClient searchServiceClient;

// 프로젝트 생성

    public String createProject(String userId, ProjectCreateDto dto) {
        // 1. 참여자 검증
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(dto.getWorkspaceId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 2. 권한 검증 (공통 메서드)
        // 커스터마이징 필요한 부분 ex. 프로젝트 관련 -> ws_acc_list_1 , 스톤 관련 -> ws_acc_list_2 등등
        if (!participant.getWorkspaceRole().equals(ADMIN)) {
            accessCheckService.validateAccess(participant, "ws_acc_list_1");
        }

        // 3. 워크스페이스 담당자 객체 생성
        WorkspaceParticipant projectManager = workspaceParticipantRepository.findById(dto.getProjectManagerId())
                .orElseThrow(()->new EntityNotFoundException("회원 정보 없습니다."));

        if (!projectManager.getWorkspace().getId().equals(dto.getWorkspaceId())) {
            throw new IllegalArgumentException("담당자는 해당 워크스페이스 소속이어야 합니다.");
        }

        // 4. 프로젝트 생성
        Workspace workspace = workspaceRepository.findById(dto.getWorkspaceId())
                .orElseThrow(()->new EntityNotFoundException("워크스페이스 정보를 찾을 수 없습니다."));
        Project project = dto.toEntity(workspace,projectManager);
        projectRepository.save(project);

        // 5. 스톤 생성
        stoneService.createTopStone(
                TopStoneCreateDto.builder()
                        .projectId(project.getId())
                        .participantId(projectManager.getId())
                        .stoneName(project.getProjectName()) //최상위 스톤 이름은 프로젝트 이름과 같게 설정
                        .workspaceId(workspace.getId())
                        .startTime(project.getStartTime())
                        .endTime(project.getEndTime())
                        .chatCreation(dto.getChatCreation())
                        .build()
        );
        return project.getId();
    }


    // 프로젝트 수정(+ 프로젝트 상태 변경)
    public String modifyProject(String userId, ProjectModifyDto dto) {
        // 1. 참여자 검증
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(dto.getWorkspaceId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 2. 프로젝트 조회
        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다."));

        // 3. 권한 검증: 담당자 or 권한그룹 or 관리자
        if (!participant.getWorkspaceRole().equals(ADMIN)) {
            if (!project.getWorkspaceParticipant().getId().equals(participant.getId())) {
                accessCheckService.validateAccess(participant, "ws_acc_list_1");
            }
        }

        // 4. 담당자 변경 처리 (선택)
        if (dto.getProjectManagerId() != null) {
            WorkspaceParticipant newManager = workspaceParticipantRepository.findByWorkspaceIdAndUserId(dto.getWorkspaceId(), dto.getProjectManagerId())
                    .orElseThrow(() -> new EntityNotFoundException("변경할 담당자 정보를 찾을 수 없습니다."));

            if (!newManager.getWorkspace().getId().equals(dto.getWorkspaceId())) {
                throw new IllegalArgumentException("담당자는 해당 워크스페이스 소속이어야 합니다.");
            }

            project.changeManager(newManager);
        }

        // 5. 최상위 스톤명 변경
        if (dto.getProjectName() != null && !dto.getProjectName().isBlank()) {
            Stone topStone = project.getStones().stream()
                    .filter(stone -> stone.getParentStoneId() == null)
                    .findFirst()
                    .orElseThrow(() -> new EntityNotFoundException("최상위 스톤이 없습니다."));

            topStone.setStoneName(dto.getProjectName());
            stoneRepository.save(topStone);
        }

        project.update(dto);
        return project.getId();
    }

// 프로젝트 목록 조회(사이드 바 프로젝트 목록)
    public List<ProjectListDto> getProjectList(String userId, String workspaceId) {
        UUID uuid = UUID.fromString(userId);

        // 1️. 워크스페이스 참여자 검증
        WorkspaceParticipant workspaceParticipant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspaceId, uuid)
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 2️. ADMIN 여부 확인
        boolean isAdmin = workspaceParticipant.getWorkspaceRole() == WorkspaceRole.ADMIN;

        List<Project> projects;

        if (isAdmin) {
            // 2-1. ADMIN은 워크스페이스 내 모든 프로젝트 조회 (삭제X, STORAGE 제외)
            projects = projectRepository
                    .findAllByWorkspaceIdAndIsDeleteFalseAndProjectStatusNot(workspaceId, ProjectStatus.STORAGE);
        } else {
            // 2-2. 일반 참여자는 본인이 포함된 프로젝트만 조회 (삭제X, STORAGE 제외)
            projects = projectParticipantRepository
                    .findProjectsByUserInWorkspace(uuid, workspaceId)
                    .stream()
                    .filter(project -> !project.getIsDelete() && project.getProjectStatus() != ProjectStatus.STORAGE)
                    .toList();
        }

        // 3. DTO 변환
        return projects.stream()
                .map(ProjectListDto::fromEntity)
                .toList();
    }


    // 프로젝트 삭제
    public void deleteProject(String userId, String projectId) {
        driveServiceClient.deleteAll("PROJECT", projectId);
        searchServiceClient.deleteAll("PROJECT", projectId);

        // 1. 프로젝트 조회
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다."));
        Workspace workspace = project.getWorkspace();

        // 2. 참여자 검증
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 3. 권한 검증
        if (participant.getWorkspaceRole() != ADMIN &&
                !project.getWorkspaceParticipant().getId().equals(participant.getId())) {
            accessCheckService.validateAccess(participant, "ws_acc_list_1");
        }

        // 4. 이미 삭제된 프로젝트인지 확인
        if (Boolean.TRUE.equals(project.getIsDelete())) {
            throw new IllegalStateException("이미 삭제된 프로젝트입니다.");
        }

        // 5. 프로젝트 내 스톤 전체 조회
        List<Stone> stones = stoneRepository.findAllByProject(project);

        for (Stone stone : stones) {
            // 5-1. 스톤 논리 삭제
            stone.setIsDelete(true);

            // 5-2. 태스크 하드삭제
            List<Task> tasks = taskRepository.findAllByStone(stone);
            if (!tasks.isEmpty()) {
                taskRepository.deleteAll(tasks);
            }

            // 5-3. 스톤 참여자 하드삭제
            List<StoneParticipant> stoneParticipants = stoneParticipantRepository.findAllByStone(stone);
            if (!stoneParticipants.isEmpty()) {
                stoneParticipantRepository.deleteAll(stoneParticipants);
            }
        }

        // 6. 프로젝트 참여자 하드삭제
        List<ProjectParticipant> projectParticipants = projectParticipantRepository.findAllByProject(project);
        if (!projectParticipants.isEmpty()) {
            projectParticipantRepository.deleteAll(projectParticipants);
        }

        // 7. 프로젝트 논리 삭제
        project.deleteProject();

        // 8. 저장
        projectRepository.save(project);
    }


    // 프로젝트 캘린더에 스톤 노출 여부 설정(프로젝트 캘린더 조회용 API)
    public void settingProject(String userId, ProjectSettingDto dto) {

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

        // 5. isProjectHidden 값 설정
        stoneParticipant.updateProjectHidden(dto.getIsProjectHidden());

        // 6. 변경사항 저장
        stoneParticipantRepository.save(stoneParticipant);
    }

// 내 프로젝트 목록 조회


// 스톤 목록? 프로젝트 내에 스톤들 뿌리처럼 보이는 거
    public List<StoneListResDto> getStoneList(String userId, String projectId) {

        // 1. 워크스페이스, 사용자 검증
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("프로젝트가 존재하지 않습니다."));

        Workspace workspace = project.getWorkspace();
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 존재하지 않습니다."));

        // 2. 프로젝트 참여자인지 검증(관리자는 프로젝트 참여자 아니어도 허용)
        if(!participant.getWorkspaceRole().equals(ADMIN)) {
            projectParticipantRepository.findByProjectAndWorkspaceParticipant(project, participant)
                    .orElseThrow(() -> new EntityNotFoundException("프로젝트 참여자가 아닙니다."));
        }
        // 3. 프로젝트 내 모든 스톤 조회 (삭제된 스톤 제외)
        List<Stone> stones = stoneRepository.findByProjectAndIsDeleteFalse(project);

        // 4. DTO 변환 후 맵 구성 (id 기준으로 빠르게 탐색)
        Map<String, StoneListResDto> dtoMap = stones.stream()
                .collect(Collectors.toMap(
                        Stone::getId,
                        s -> StoneListResDto.builder()
                                .projectId(projectId)
                                .stoneId(s.getId())
                                .stoneName(s.getStoneName())
                                .startTime(s.getStartTime())
                                .endTime(s.getEndTime())
                                .createdAt(s.getCreatedAt())
                                .parentStoneId(s.getParentStoneId() != null ? s.getParentStoneId() : null)
                                .childStone(new ArrayList<>()) // 초기화
                                .milestone(s.getMilestone())
                                .build()
                ));

        // 5. 트리 구성
        List<StoneListResDto> roots = new ArrayList<>();

        for (StoneListResDto dto : dtoMap.values()) {
            if (dto.getParentStoneId() == null) {
                // 부모가 없는 스톤 = 루트
                roots.add(dto);
            } else {
                // 부모가 있으면 부모의 child 리스트에 추가
                StoneListResDto parent = dtoMap.get(dto.getParentStoneId());
                if (parent != null) {
                    parent.getChildStone().add(dto);
                }
            }
        }

        // 6. 트리 형태로 반환
        return roots;
    }

    // 프로젝트 상세조회
    @Transactional(readOnly = true)
    public ProjectDetailResDto getProjectDetail(String userId, String projectId) {

        // 1. 프로젝트 조회
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다."));

        // 2. 워크스페이스 및 사용자 검증
        Workspace workspace = project.getWorkspace();
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 3. 권한 검증
        // 관리자이거나 프로젝트 참여자면 통과
        boolean isAdmin = participant.getWorkspaceRole().equals(ADMIN);
        boolean isProjectParticipant = projectParticipantRepository
                .existsByProjectAndWorkspaceParticipant(project, participant);

        if (!isAdmin && !isProjectParticipant) {
            accessCheckService.validateAccess(participant, "ws_acc_list_1"); // 프로젝트 접근 권한 체크
        }

        // 4. DTO 변환 및 반환
        return ProjectDetailResDto.builder()
                .projectId(project.getId())
                .projectName(project.getProjectName())
                .projectObjective(project.getProjectObjective())
                .projectDescription(project.getProjectDescription())
                .milestone(project.getMilestone())
                .startTime(project.getStartTime())
                .endTime(project.getEndTime())
                .projectStatus(project.getProjectStatus())
                .isDelete(project.getIsDelete())
                .stoneCount(project.getStoneCount())
                .completedCount(project.getCompletedCount())
                .projectManagerName(project.getWorkspaceParticipant().getUserName())
                .build();
    }

    // 프로젝트 대시보드용 인원 현황 API
    @Transactional(readOnly = true)
    public ProjectPeopleOverviewResDto getProjectPeopleOverview(String userId, String projectId) {

        // 1. 프로젝트 조회 + 삭제 체크
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("해당 프로젝트가 존재하지 않습니다."));
        if (Boolean.TRUE.equals(project.getIsDelete())) {
            throw new IllegalArgumentException("삭제된 프로젝트입니다.");
        }

        // 2. 프로젝트의 스톤(삭제 제외) 수집
        List<Stone> stones = project.getStones().stream()
                .filter(s -> !Boolean.TRUE.equals(s.getIsDelete()))
                .toList();

        // 3. 사람 수집(담당 + 참여)
        Map<UUID, List<SimpleStoneRefDto>> ownedMap = new HashMap<>();
        Map<UUID, List<SimpleStoneRefDto>> partMap  = new HashMap<>();

        for (Stone s : stones) {
            // 3-1) 담당자: 루트 스톤(parentStoneId == null)은 제외
            if (s.getParentStoneId() != null
                    && s.getStoneManager() != null
                    && !Boolean.TRUE.equals(s.getStoneManager().getIsDelete())) {
                UUID uid = s.getStoneManager().getUserId();
                ownedMap.computeIfAbsent(uid, k -> new ArrayList<>())
                        .add(SimpleStoneRefDto.builder()
                                .stoneId(s.getId())
                                .stoneName(s.getStoneName())
                                .build());
            }

            // 3-2) 참여자: 그대로(루트 포함, 삭제되지 않은 참여자만)
            List<StoneParticipant> sps = stoneParticipantRepository
                    .findAllByStoneAndWorkspaceParticipant_IsDeleteFalse(s);
            for (StoneParticipant sp : sps) {
                UUID uid = sp.getWorkspaceParticipant().getUserId();
                partMap.computeIfAbsent(uid, k -> new ArrayList<>())
                        .add(SimpleStoneRefDto.builder()
                                .stoneId(s.getId())
                                .stoneName(s.getStoneName())
                                .build());
            }
        }

        // 4. 전체 유저 집합
        Set<UUID> allUserIds = new HashSet<>();
        allUserIds.addAll(ownedMap.keySet());
        allUserIds.addAll(partMap.keySet());
        if (allUserIds.isEmpty()) {
            return ProjectPeopleOverviewResDto.builder()
                    .totalPeopleCount(0).managerCount(0).participantOnlyCount(0)
                    .people(List.of()).build();
        }

        // 5. 유저 프로필 일괄 조회
        Map<UUID, UserInfoResDto> userMap = userFeign
                .fetchUserListInfo(new UserIdListDto(new ArrayList<>(allUserIds)))
                .getUserInfoList().stream()
                .collect(Collectors.toMap(UserInfoResDto::getUserId, u -> u));

        // 6. 프로젝트 내 태스크 담당자별 집계
        List<Task> tasksInProject = taskRepository.findAllByProjectId(project.getId());
        Map<UUID, Integer> taskTotalByUser = new HashMap<>();
        Map<UUID, Integer> taskDoneByUser  = new HashMap<>();
        for (Task t : tasksInProject) {
            if (t.getTaskManager() == null || Boolean.TRUE.equals(t.getTaskManager().getIsDelete())) continue;
            UUID uid = t.getTaskManager().getUserId();
            taskTotalByUser.merge(uid, 1, Integer::sum);
            if (Boolean.TRUE.equals(t.getIsDone())) taskDoneByUser.merge(uid, 1, Integer::sum);
        }

        // 7. DTO 조립
        List<ProjectMemberOverviewDto> people = new ArrayList<>();
        for (UUID uid : allUserIds) {
            var user = userMap.get(uid);
            List<SimpleStoneRefDto> owned = ownedMap.getOrDefault(uid, List.of());
            List<SimpleStoneRefDto> part  = partMap.getOrDefault(uid, List.of());

            people.add(ProjectMemberOverviewDto.builder()
                    .user(user)
                    .ownedStoneCount(owned.size())
                    .participatingStoneCount(part.size())
                    .ownedStones(owned)
                    .participatingStones(part)
                    .myTaskTotal(taskTotalByUser.getOrDefault(uid, 0))
                    .myTaskCompleted(taskDoneByUser.getOrDefault(uid, 0))
                    .build());
        }

        int managerCount = (int) people.stream()
                .filter(p -> p.getOwnedStoneCount() != null && p.getOwnedStoneCount() > 0)
                .count();
        int total = people.size();

        return ProjectPeopleOverviewResDto.builder()
                .totalPeopleCount(total)
                .managerCount(managerCount)
                .participantOnlyCount(total - managerCount)
                .people(people.stream()
                        .sorted(Comparator
                                .comparing(ProjectMemberOverviewDto::getOwnedStoneCount,
                                        Comparator.nullsFirst(Comparator.reverseOrder()))
                                .thenComparing(p -> Optional.ofNullable(p.getUser())
                                        .map(UserInfoResDto::getUserName).orElse("")))
                        .toList())
                .build();
    }

    // 프로젝트 stone, task 수 조회 API
    @Transactional(readOnly = true)
    public ProjectDashboardResDto getProjectDashboard(String userId, String projectId) {

        // 1) 프로젝트 존재/삭제 체크
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("해당 프로젝트가 존재하지 않습니다."));
        if (Boolean.TRUE.equals(project.getIsDelete())) {
            throw new IllegalArgumentException("삭제된 프로젝트입니다.");
        }

        // 2) 스톤 집계: 루트 제외 + 삭제 제외
        long totalStoneCount = stoneRepository.countActiveNonRootByProjectId(projectId);
        long completedStoneCount = stoneRepository.countCompletedNonRootByProjectId(projectId);

        // 3) 태스크 집계: 삭제 스톤 제외
        long totalTaskCount = taskRepository.countTasksByProjectId(projectId);
        long completedTaskCount = taskRepository.countDoneTasksByProjectId(projectId);

        // 4) 진행률은 엔티티의 milestone을 그대로 사용

    // 추가된 부분 데이터 계산

        // 1. 평균 태스크 완료시간(일 단위, 반올림)
        Double avgSec = taskRepository.avgCompletionSecondsByProjectId(projectId);
        int avgTaskCompletedTime = (avgSec == null) ? 0 : (int) Math.round(avgSec / 86400.0);

        // 2. 태스크 리스크 지수(밀린태스크 수 / 전체 태스크 수)
        LocalDateTime now = LocalDateTime.now();
        long delayedOpen    = taskRepository.countDelayedOpenByProjectId(projectId, now);

        BigDecimal taskRisk = (totalTaskCount > 0)
                ? BigDecimal.valueOf(delayedOpen)
                .divide(BigDecimal.valueOf(totalTaskCount), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 지연되고 있는 태스크 리스트
        List<Task> delayed = taskRepository
                .findAllByStone_Project_IdAndIsDoneFalseAndEndTimeBeforeOrderByEndTimeAsc(projectId, now);


        List<ProjectDashboardResDto.LazyTask> lazyTasklist = delayed.stream()
                .map(t -> ProjectDashboardResDto.LazyTask.builder()
                        .taskName(t.getTaskName())
                        .lazyDay((int) java.time.temporal.ChronoUnit.DAYS.between(t.getEndTime(), now))
                        .taskManagerName(
                                (t.getTaskManager() != null) ? t.getTaskManager().getUserName() : null
                        )
                        .build()
                )
                .toList();

        // 완료된 스톤들 정보
        List<ProjectDashboardResDto.CompletedStone> completedStoneList =
                project.getStones().stream()
                        .filter(s -> !Boolean.TRUE.equals(s.getIsDelete()))
                        // 루트 스톤 제외하려면
                        .filter(s -> s.getParentStoneId() != null)
                        .filter(s -> s.getStatus().equals(StoneStatus.COMPLETED))
                        .sorted(Comparator.comparing(Stone::getUpdatedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                        .map(s -> ProjectDashboardResDto.CompletedStone.builder()
                                .stoneId(s.getId())
                                .stoneName(s.getStoneName())
                                .stoneCompletedDay(s.getUpdatedAt())
                                .build())
                        .toList();

        // 완료된 태스크들 정보
        List<ProjectDashboardResDto.CompletedTask> completedTaskList =
                taskRepository.findAllByProjectIdAndIsDoneTrueOrderByCompletedAtDesc(projectId).stream()
                        .map(t -> ProjectDashboardResDto.CompletedTask.builder()
                                .taskId(t.getId())
                                .taskName(t.getTaskName())
                                .taskCompletedDay(t.getTaskCompletedDate())
                                .build())
                        .toList();

        // 프로젝트 문서 수 + 파일 용량
        CommonSuccessDto body = driveServiceClient.getElements(projectId).getBody();
        if (body == null || body.getResult() == null) {
            throw new IllegalStateException("드라이브 요소 조회 결과가 없습니다.");
        }
        ElementCountAndSizeResDto elementCountAndSizeResDto =
                objectMapper.convertValue(body.getResult(), ElementCountAndSizeResDto.class);


        return ProjectDashboardResDto.builder()
                .projectMilestone(project.getMilestone())
                .totalStoneCount((int) totalStoneCount)
                .completedStoneCount((int) completedStoneCount)
                .totalTaskCount((int) totalTaskCount)
                .completedTaskCount((int) completedTaskCount)
                .avgTaskCompletedTime(avgTaskCompletedTime)   // 단위: '일'
                .taskRisk(taskRisk)
                .lazyTasklist(lazyTasklist)
                .completedStoneList(completedStoneList)
                .completedTaskList(completedTaskList)
                .elementCountAndSizeResDto(elementCountAndSizeResDto) // 추가
                .build();
    }

    // LLM을 위한 프로젝트 요약 DTO 반환 API
    public ProjectSnapshotDto getProjectLLM(String userId, String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(()-> new EntityNotFoundException("프로젝트가 없습니다."));
        Workspace workspace = project.getWorkspace();
        WorkspaceParticipant workspaceParticipant = workspaceParticipantRepository.findByWorkspaceIdAndUserId(workspace.getId(),UUID.fromString(userId))
                .orElseThrow(()-> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        LocalDateTime now = LocalDateTime.now();

        // 1. 프로젝트 관련 dto
        ProjectSnapshotDto.ProjectDto projectDto = ProjectSnapshotDto.ProjectDto.builder()
                .name(project.getProjectName())
                .currentDate(now)
                .startDate(project.getStartTime())
                .dueDate(project.getEndTime())
                .build();

        // 2. 프로젝트 진행률 관련 dto
        long total = taskRepository.countAllByProjectId(projectId);
        long completed = taskRepository.countCompletedByProjectId(projectId);
        long remaining = Math.max(0, total - completed);
        long delayed = taskRepository.countDelayedOpenByProjectId(projectId, now);
        long reopened = taskRepository.sumReopenedByProjectId(projectId);

        // 평균 완료시간(초 → 일)
        Double avgSec = taskRepository.avgCompletionSecondsByProjectId(projectId);
        double avgDays = (avgSec == null ? 0d : avgSec / 86400.0); // 86400초 = 1일

        long last7 = taskRepository.countCompletedBetween(projectId, now.minusDays(7), now.plusSeconds(1));
        long last30 = taskRepository.countCompletedBetween(projectId, now.minusDays(30), now.plusSeconds(1));

        ProjectSnapshotDto.ProgressDto progressDto = ProjectSnapshotDto.ProgressDto.builder()
                .totalTasks((int) total)
                .completedTasks((int) completed)
                .remainingTasks((int) remaining)
                .averageTaskCompletionTimeDays(round1(avgDays))
                .recentCompletionRate(
                        ProjectSnapshotDto.ProgressDto.RecentCompletionRateDto.builder()
                                .last7Days((int) last7)
                                .last30Days((int) last30)
                                .build()
                )
                .delayedTasks((int) delayed)
                .reopenedTasks((int) reopened)
                .build();

        // 3. 프로젝트 참여자 정보 dto
        int totalMembers = projectParticipantRepository.findAllByProject(project).size();
        long activeMembers = projectParticipantRepository.countActiveByProjectId(project.getId());

        // 담당자별 완료율 평균(%) = 각 담당자 (완료/전체)의 산술평균
        List<Object[]> rows = taskRepository.countTotalsAndCompletedByManager(project.getId()); // [managerId, totalCnt, completedCnt]
        double sumRatios = 0.0;
        int managerWithTasks = 0;
        for (Object[] r : rows) {
            long totalr = (Long) r[1];
            long done  = (r[2] == null) ? 0L : (Long) r[2];
            if (totalr > 0) {
                sumRatios += ((double) done / totalr); // 0.0 ~ 1.0
                managerWithTasks++;
            }
        }
        double avgCompletionRatePercent = (managerWithTasks == 0)
                ? 0.0
                : Math.round((sumRatios / managerWithTasks) * 1000.0) / 10.0; // 소수1자리 %

        // workloadDeviation(CoV) = 멤버별 태스크 수 표준편차 / 평균 태스크 수
        long totalTasks = taskRepository.countAllByProjectId(project.getId());
        double avgPerMemberTasks = (totalMembers == 0) ? 0.0 : (double) totalTasks / totalMembers;

        List<Object[]> buckets = taskRepository.countTasksGroupedByManager(project.getId()); // [managerId, cnt]
        List<Long> counts = buckets.stream().map(r -> (Long) r[1]).toList();
        double stdev;
        if (counts.isEmpty()) {
            stdev = 0.0;
        } else {
            double mean = counts.stream().mapToDouble(Long::doubleValue).average().orElse(0.0);
            double variance = counts.stream()
                    .mapToDouble(x -> {
                        double d = x - mean;
                        return d * d;
                    })
                    .sum() / counts.size(); // 모집단 분산
            stdev = Math.sqrt(variance);
        }
        double cov = (avgPerMemberTasks == 0.0) ? 0.0 : (stdev / avgPerMemberTasks);
        double covRounded = new java.math.BigDecimal(cov).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();

        // DTO 조립 (averageTasksPerMember를 "담당자별 완료율 평균(%)"의 정수값으로 사용)
        int averageTaskPerMember = (int) Math.round(avgCompletionRatePercent);

        ProjectSnapshotDto.TeamDto teamDto = ProjectSnapshotDto.TeamDto.builder()
                .totalMembers(totalMembers)
                .activeMembers((int)activeMembers)
                .averageTasksPerMember(averageTaskPerMember)  // 예: 42 (%)
                .workloadDeviation(covRounded)                // 예: 0.25
                .build();



        ProjectSnapshotDto.ContextDto contextDto = ProjectSnapshotDto.ContextDto.builder()
                .weekendsExcluded(true)
                .build();

        ProjectSnapshotDto dto = ProjectSnapshotDto.builder()
                .project(projectDto)
                .team(teamDto)
                .progress(progressDto)
                .context(contextDto)
                .build();



        return dto;
    }




    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    // 소수 2자리 반올림 (workloadDeviation용)
    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }




}
