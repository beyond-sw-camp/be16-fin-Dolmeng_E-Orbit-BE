package com.Dolmeng_E.workspace.domain.task.service;

import com.Dolmeng_E.workspace.common.controller.DriveServiceClient;
import com.Dolmeng_E.workspace.common.controller.SearchServiceClient;
import com.Dolmeng_E.workspace.common.domain.NotificationType;
import com.Dolmeng_E.workspace.common.dto.NotificationCreateReqDto;
import com.Dolmeng_E.workspace.common.service.MilestoneCalculator;
import com.Dolmeng_E.workspace.common.service.NotificationKafkaService;
import com.Dolmeng_E.workspace.domain.project.entity.Project;
import com.Dolmeng_E.workspace.domain.project.repository.ProjectRepository;
import com.Dolmeng_E.workspace.domain.stone.entity.Stone;
import com.Dolmeng_E.workspace.domain.stone.entity.StoneStatus;
import com.Dolmeng_E.workspace.domain.stone.repository.StoneParticipantRepository;
import com.Dolmeng_E.workspace.domain.stone.repository.StoneRepository;
import com.Dolmeng_E.workspace.domain.task.dto.TaskCreateDto;
import com.Dolmeng_E.workspace.domain.task.dto.TaskModifyDto;
import com.Dolmeng_E.workspace.domain.task.dto.TaskResDto;
import com.Dolmeng_E.workspace.domain.task.entity.Task;
import com.Dolmeng_E.workspace.domain.task.repository.TaskRepository;
import com.Dolmeng_E.workspace.domain.workspace.dto.DriveKafkaReqDto;
import com.Dolmeng_E.workspace.domain.workspace.entity.Workspace;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceRole;
import com.Dolmeng_E.workspace.domain.workspace.repository.WorkspaceParticipantRepository;
import com.Dolmeng_E.workspace.domain.workspace.repository.WorkspaceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final StoneRepository stoneRepository;
    private final ProjectRepository projectRepository;
    private final WorkspaceParticipantRepository workspaceParticipantRepository;
    private final WorkspaceRepository workspaceRepository;
    private final StoneParticipantRepository stoneParticipantRepository;
    private final MilestoneCalculator milestoneCalculator;
    private final NotificationKafkaService notificationKafkaService;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final DriveServiceClient driveServiceClient;
    private final SearchServiceClient searchServiceClient;

    // 태스크 생성(생성시 스톤의 task수 반영 필요)
    public String createTask(String userId, TaskCreateDto dto) {

        // 1. 스톤관련 객체 조회
        Stone stone = stoneRepository.findById(dto.getStoneId())
                .orElseThrow(()-> new EntityNotFoundException("스톤이 존재하지 않습니다."));
        Project project = projectRepository.findById(stone.getProject().getId())
                .orElseThrow(()-> new EntityNotFoundException("프로젝트가 존재하지 않습니다."));
        Workspace workspace = workspaceRepository.findById(project.getWorkspace().getId())
                .orElseThrow(()-> new EntityNotFoundException("워크스페이스가 존재하지 않습니다."));
        WorkspaceParticipant requester = workspaceParticipantRepository.findByWorkspaceIdAndUserId(workspace.getId(), UUID.fromString(userId))
                .orElseThrow(()-> new EntityNotFoundException("워크스페이스 참여자 정보가 없습니다."));

        // 2. 권한조회(관리자,프로젝트 담당자, 스톤담당자만 허용)
        boolean isAdmin = requester.getWorkspaceRole().equals(WorkspaceRole.ADMIN);
        boolean isProjectManager = project.getWorkspaceParticipant().equals(requester);
        boolean isStoneManager = stone.getStoneManager().equals(requester);

        // 셋 중 하나라도 true면 통과
        if (!isAdmin && !isProjectManager && !isStoneManager) {
            throw new IllegalArgumentException("태스크 생성 권한이 없습니다.");
        }

        // 스톤이 최상위 스톤이면 task 생성 불가
        if(stone.getParentStoneId()==null) {
            throw new IllegalArgumentException("최상위 스톤은 태스크 생성 불가합니다.");
        }

        // 스톤이 완료처리 되었다면 Task 생성 불가
        if(stone.getStatus().equals(StoneStatus.COMPLETED)) {
            throw new IllegalArgumentException("완료처리된 스톤에 task 생성 불가능합니다.");
        }

        // 3. 태스크 담당자 검증(스톤 참여자 혹은 스톤 담당자만 가능)
        WorkspaceParticipant taskManager = workspaceParticipantRepository.findByWorkspaceIdAndUserId(workspace.getId(), dto.getManagerId())
                .orElseThrow(()->new EntityNotFoundException("워크스페이스 참여자 정보가 없습니다."));
        // 스톤 참여자 목록에 dto의 담당자 id가 있는지 조회
        if(!stoneParticipantRepository.existsByStoneAndWorkspaceParticipant(stone,taskManager)) {
            throw new EntityNotFoundException("스톤 참여자 목록에 담당자 id가 존재하지 않습니다.");
        }
        // 스톤이 완료상태라면 task 추가 x
        if(stone.getStatus().equals(StoneStatus.COMPLETED)) {
            throw new IllegalArgumentException("스톤이 이미 완료상태입니다.");
        }

        // 4. 태스크 기간 검증 (스톤 기간 내에만 생성 가능)
        if (dto.getStartTime().isBefore(stone.getStartTime()) ||
                dto.getEndTime().isAfter(stone.getEndTime())) {
            throw new IllegalArgumentException("스톤 기간 내에만 스톤 생성이 가능합니다.");
        }

        // 5. 태스크 생성 및 저장
                Task task = Task.builder()
                        .taskName(dto.getTaskName())
                        .stone(stone)
                        .taskManager(taskManager)
                        .isDone(false)
                        .startTime(dto.getStartTime())
                        .endTime(dto.getEndTime())
                        .build();

                taskRepository.save(task);

        // task 담당자에게 알림 발송

        // 테스트 코드
        List<UUID> userIdList = new ArrayList<>();
        // 알림받을 인원들 list에 담고
        userIdList.add(task.getTaskManager().getUserId());

        // 객체 생성
        NotificationCreateReqDto notificationCreateReqDto = NotificationCreateReqDto.builder()
                // 워크스페이스명 수동으로 넣어줘야 해요
                .title("[" + workspace.getWorkspaceName() + "]" + "태스크 배정")
                .content(task.getTaskName() + " 태스크가 배정되었습니다! 🎉")
                .userIdList(userIdList)
                // 위에서 추가한 알림 타입 String으로 주입
                .type("TASK_MESSAGE")
                // 예약 알림이라면 원하는 날짜 지정 (예. 만료기한날짜 -1일 등)
                // 즉시알림이라면 null (채팅같은)
                .sendAt(null)
                .workspaceId(workspace.getId())
                .taskId(task.getId())
                .stoneId(stone.getId())
                .projectId(project.getId())
                .build();

        notificationKafkaService.kafkaNotificationPublish(notificationCreateReqDto);

        // 6. 스톤의 태스크 수 갱신
        stone.incrementTaskCount();
        stoneRepository.save(stone);

        // 7. 마일스톤 업데이트
        milestoneCalculator.updateStoneAndParents(stone);

        return task.getId();

    }

    // 태스크 수정
    public String modifyTask(String userId, TaskModifyDto dto) {

        // 1. 태스크 조회
        Task task = taskRepository.findById(dto.getTaskId())
                .orElseThrow(() -> new EntityNotFoundException("태스크를 찾을 수 없습니다."));

        Stone stone = task.getStone();
        Project project = stone.getProject();
        Workspace workspace = project.getWorkspace();

        // 2. 요청자 조회
        WorkspaceParticipant requester = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자 정보를 찾을 수 없습니다."));

        // 3. 권한검증 (관리자, 프로젝트 담당자, 스톤 담당자, 태스크 담당자 허용)
        boolean isAdmin = requester.getWorkspaceRole().equals(WorkspaceRole.ADMIN);
        boolean isProjectManager = project.getWorkspaceParticipant().equals(requester);
        boolean isStoneManager = stone.getStoneManager().equals(requester);
        boolean isTaskManager = task.getTaskManager().equals(requester);

        if (!isAdmin && !isProjectManager && !isStoneManager && !isTaskManager) {
            throw new IllegalArgumentException("태스크 수정 권한이 없습니다.");
        }

        // 태스크 기간 검증 (스톤 기간 내에만 수정 가능)
        if (dto.getStartTime().isBefore(stone.getStartTime()) ||
                dto.getEndTime().isAfter(stone.getEndTime())) {
            throw new IllegalArgumentException("스톤 기간 내에만 스톤 수정이 가능합니다.");
        }

        // 4. 수정 가능한 필드만 변경
        if (dto.getTaskName() != null) task.setTaskName(dto.getTaskName());
        if (dto.getStartTime() != null) task.setStartTime(dto.getStartTime());
        if (dto.getEndTime() != null) task.setEndTime(dto.getEndTime());

        // 5. 태스크 담당자 교체 (선택적)
        if (dto.getNewManagerUserId() != null) {
            WorkspaceParticipant newManager = workspaceParticipantRepository
                    .findByWorkspaceIdAndUserId(workspace.getId(), dto.getNewManagerUserId())
                    .orElseThrow(() -> new EntityNotFoundException("새 담당자 정보를 찾을 수 없습니다."));

            // 스톤 참여자 검증
            if (!stoneParticipantRepository.existsByStoneAndWorkspaceParticipant(stone, newManager)) {
                throw new IllegalArgumentException("해당 스톤의 참여자가 아닙니다.");
            }

            task.setTaskManager(newManager);

            // task 담당자에게 알림 발송

            // 테스트 코드
            List<UUID> userIdList = new ArrayList<>();
            // 알림받을 인원들 list에 담고
            userIdList.add(newManager.getUserId());

            // 객체 생성
            NotificationCreateReqDto notificationCreateReqDto = NotificationCreateReqDto.builder()
                    // 워크스페이스명 수동으로 넣어줘야 해요
                    .title("[" + workspace.getWorkspaceName() + "]" + "태스크 배정")
                    .content(task.getTaskName() + " 태스크가 배정되었습니다! 🎉")
                    .userIdList(userIdList)
                    // 위에서 추가한 알림 타입 String으로 주입
                    .type("TASK_MESSAGE")
                    // 예약 알림이라면 원하는 날짜 지정 (예. 만료기한날짜 -1일 등)
                    // 즉시알림이라면 null (채팅같은)
                    .sendAt(null)
                    .workspaceId(workspace.getId())
                    .taskId(task.getId())
                    .stoneId(stone.getId())
                    .projectId(project.getId())
                    .build();
        }

        // 6. 변경사항 저장
        taskRepository.save(task);

        return task.getId();
    }


    // 태스크 삭제(삭제시 스톤의 task수 반영 필요)
    public void deleteTask(String userId, String taskId) {
        driveServiceClient.deleteAll("TASK", taskId);
        searchServiceClient.deleteAll("TASK", taskId);
        // 1. 태스크 조회
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("태스크를 찾을 수 없습니다."));

        Stone stone = task.getStone();
        Project project = stone.getProject();
        Workspace workspace = project.getWorkspace();

        // 2. 요청자 조회
        WorkspaceParticipant requester = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자 정보를 찾을 수 없습니다."));

        // 3. 권한검증 (관리자, 프로젝트 담당자, 스톤 담당자, 태스크 담당자 허용)
        boolean isAdmin = requester.getWorkspaceRole().equals(WorkspaceRole.ADMIN);
        boolean isProjectManager = project.getWorkspaceParticipant().equals(requester);
        boolean isStoneManager = stone.getStoneManager().equals(requester);
        boolean isTaskManager = task.getTaskManager().equals(requester);

        if (!isAdmin && !isProjectManager && !isStoneManager && !isTaskManager) {
            throw new IllegalArgumentException("태스크 삭제 권한이 없습니다.");
        }

        // 4. 태스크 삭제(hard delete)
        taskRepository.delete(task);

        // 5. 스톤에서 task 수 감소
        stone.decrementTaskCount();

        // 6. 마일스톤 갱신
        milestoneCalculator.updateStoneAndParents(stone);

        // kafka 메시지 발행
        DriveKafkaReqDto driveKafkaReqDto = DriveKafkaReqDto.builder()
                .rootId(taskId)
                .rootType("TASK")
                .build();
        try {
            // 3. DTO를 JSON 문자열로 변환
            String message = objectMapper.writeValueAsString(driveKafkaReqDto);

            // 4. Kafka 토픽으로 이벤트 발행
            kafkaTemplate.send("drive-delete-topic", message);

        } catch (JsonProcessingException e) {
            // 예외 처리 (심각한 경우 트랜잭션 롤백 고려)
        }
    }


    // 태스크 완료 처리(완료시 스톤의 마일스톤 반영 필요)

    public BigDecimal completeTask(String userId, String taskId) {
        // 1. 태스크 조회
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("태스크를 찾을 수 없습니다."));

        Stone stone = task.getStone();
        Project project = stone.getProject();
        Workspace workspace = project.getWorkspace();

        // 2. 요청자 조회
        WorkspaceParticipant requester = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자 정보를 찾을 수 없습니다."));

        // 3. 권한검증 (관리자, 프로젝트 담당자, 스톤 담당자, 태스크 담당자 허용)
        // Memo: 태스크 담당자만 완료처리 하려 했는데, 인수인계가 안되었을 때 대비해서 권한 설정
        boolean isAdmin = requester.getWorkspaceRole().equals(WorkspaceRole.ADMIN);
        boolean isProjectManager = project.getWorkspaceParticipant().equals(requester);
        boolean isStoneManager = stone.getStoneManager().equals(requester);
        boolean isTaskManager = task.getTaskManager().equals(requester);

        if (!isAdmin && !isProjectManager && !isStoneManager && !isTaskManager) {
            throw new IllegalArgumentException("태스크 삭제 권한이 없습니다.");
        }

        // 4. 태스크 완료 상태 변경
        if(!task.getIsDone()) {
            task.setIsDone(Boolean.TRUE);
        } else {
            throw new IllegalArgumentException("이미 완료된 태스크입니다.");
        }

        // stone 담당자에게 알림 발송

        // 테스트 코드
        List<UUID> userIdList = new ArrayList<>();
        // 알림받을 인원들 list에 담고
        userIdList.add(stone.getStoneManager().getUserId());

        // 객체 생성
        NotificationCreateReqDto notificationCreateReqDto = NotificationCreateReqDto.builder()
                // 워크스페이스명 수동으로 넣어줘야 해요
                .title("[" + workspace.getWorkspaceName() + "]" + "하위 태스크 완료")
                .content(task.getTaskName() + " 태스크가 완료되었습니다! 🎉")
                .userIdList(userIdList)
                // 위에서 추가한 알림 타입 String으로 주입
                .type("TASK_MESSAGE")
                // 예약 알림이라면 원하는 날짜 지정 (예. 만료기한날짜 -1일 등)
                // 즉시알림이라면 null (채팅같은)
                .sendAt(null)
                .projectId(project.getId())
                .workspaceId(workspace.getId())
                .stoneId(stone.getId())
                .build();

        // 5. 스톤의 완료된 태스크 수 증가
        stone.incrementCompletedCount();
        stoneRepository.save(stone);

        // 6. 마일스톤(진척도) 반영
        milestoneCalculator.updateStoneAndParents(task.getStone());

        return stone.getMilestone();

    }

    // 태스크 목록 조회
    public List<TaskResDto> getTaskList(String userId, String stoneId) {

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

        // 4. 접근 권한 검증
        boolean isAuthorized =
                participant.getWorkspaceRole().equals(WorkspaceRole.ADMIN) ||
                        project.getWorkspaceParticipant().getId().equals(participant.getId()) ||
                        stone.getStoneManager().getId().equals(participant.getId()) ||
                        stoneParticipantRepository.existsByStoneAndWorkspaceParticipant(stone, participant);

        if (!isAuthorized) {
            throw new IllegalArgumentException("해당 스톤에 접근할 권한이 없습니다.");
        }

        // 5. 태스크 목록 조회 로직
        List<Task> taskList = taskRepository.findAllByStone(stone);

        List<TaskResDto> result = taskList.stream()
                .map(task -> TaskResDto.builder()
                        .taskId(task.getId())
                        .taskManagerId(task.getTaskManager().getId())
                        .taskManagerUserId(task.getTaskManager().getUserId())
                        .taskName(task.getTaskName())
                        .startTime(task.getStartTime())
                        .endTime(task.getEndTime())
                        .isDone(task.getIsDone())
                        .taskManagerName(task.getTaskManager().getUserName()) // 담당자 이름 추가
                        .build()
                )
                .toList();

        return result;
    }

    // 태스크 취소
    public String cancelTask(String userId, String taskId) {
        // 1. 태스크 조회
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("태스크를 찾을 수 없습니다."));

        Stone stone = task.getStone();
        Project project = stone.getProject();
        Workspace workspace = project.getWorkspace();

        // 2. 요청자 조회
        WorkspaceParticipant requester = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자 정보를 찾을 수 없습니다."));

        // 3. 권한검증 (관리자, 프로젝트 담당자, 스톤 담당자, 태스크 담당자 허용)
        boolean isAdmin = requester.getWorkspaceRole().equals(WorkspaceRole.ADMIN);
        boolean isProjectManager = project.getWorkspaceParticipant().equals(requester);
        boolean isStoneManager = stone.getStoneManager().equals(requester);
        boolean isTaskManager = task.getTaskManager().equals(requester);

        if (!isAdmin && !isProjectManager && !isStoneManager && !isTaskManager) {
            throw new IllegalArgumentException("태스크 취소 권한이 없습니다.");
        }

        // 4. 이미 미완료 상태면 취소 불필요
        if (!task.getIsDone()) {
            throw new IllegalArgumentException("이미 미완료 상태의 태스크입니다.");
        }

        // 5. 태스크 상태 변경
        task.setIsDone(false);
        taskRepository.save(task);

        // 6. 스톤 완료된 태스크 수 감소
        stone.decrementTaskCount();
        stoneRepository.save(stone);

        // 7. 마일스톤 재계산
        milestoneCalculator.updateStoneAndParents(stone);

        // 8. 알림 전송 (스톤 담당자에게)
        List<UUID> userIdList = new ArrayList<>();
        userIdList.add(stone.getStoneManager().getUserId());

        NotificationCreateReqDto notificationCreateReqDto = NotificationCreateReqDto.builder()
                .title("[" + workspace.getWorkspaceName() + "] 태스크 취소 알림")
                .content("태스크가 취소되었습니다. (" + task.getTaskName() + ")")
                .userIdList(userIdList)
                .type("TASK_MESSAGE")
                .sendAt(null)
                .workspaceId(workspace.getId())
                .projectId(project.getId())
                .stoneId(stone.getId())
                .taskId(task.getId())
                .build();

        notificationKafkaService.kafkaNotificationPublish(notificationCreateReqDto);

        return task.getId();
    }

}
