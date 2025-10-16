package com.Dolmeng_E.workspace.domain.task.service;

import com.Dolmeng_E.workspace.domain.project.entity.Project;
import com.Dolmeng_E.workspace.domain.project.repository.ProjectRepository;
import com.Dolmeng_E.workspace.domain.stone.entity.Stone;
import com.Dolmeng_E.workspace.domain.stone.entity.StoneStatus;
import com.Dolmeng_E.workspace.domain.stone.repository.StoneParticipantRepository;
import com.Dolmeng_E.workspace.domain.stone.repository.StoneRepository;
import com.Dolmeng_E.workspace.domain.task.dto.TaskCreateDto;
import com.Dolmeng_E.workspace.domain.task.dto.TaskModifyDto;
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

        // 3. 태스크 담당자 검증(스톤 참여자 혹은 스톤 담당자만 가능)
        WorkspaceParticipant taskManager = workspaceParticipantRepository.findById(dto.getManagerId())
                .orElseThrow(()->new EntityNotFoundException("워크스페이스 참여자 정보가 없습니다."));
        // 스톤 참여자 목록에 dto의 담당자 id가 있는지 조회
        if(!stoneParticipantRepository.existsByStoneAndWorkspaceParticipant(stone,taskManager)) {
            throw new EntityNotFoundException("스톤 참여자 목록에 담당자 id가 존재하지 않습니다.");
        }
        // 스톤이 완료상태라면 task 추가 x
        if(stone.getStatus().equals(StoneStatus.COMPLETED)) {
            throw new IllegalArgumentException("스톤이 이미 완료상태입니다.");
        }

        // 4. 태스크 생성 및 저장
                Task task = Task.builder()
                        .taskName(dto.getTaskName())
                        .stone(stone)
                        .taskManager(taskManager)
                        .isDone(false)
                        .startTime(dto.getStartTime())
                        .endTime(dto.getEndTime())
                        .build();

                taskRepository.save(task);

        // 5. 스톤의 태스크 수 갱신
        stone.incrementTaskCount();

        // 6. 마일스톤 업데이트
        stone.updateMilestone();

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

        // 4. 수정 가능한 필드만 변경
        if (dto.getTaskName() != null) {
            task.setTaskName(dto.getTaskName());
        }
        if (dto.getStartTime() != null) {
            task.setStartTime(dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            task.setEndTime(dto.getEndTime());
        }

        return task.getId();
    }

    // 태스크 삭제(삭제시 스톤의 task수 반영 필요)
    public void deleteTask(String userId, String taskId) {
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
        stone.updateMilestone();
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

        // 5. 스톤의 완료된 태스크 수 증가
        stone.incrementCompletedCount();

        // 6. 마일스톤(진척도) 반영
        stone.updateMilestone();

        return stone.getMilestone();

    }
}
