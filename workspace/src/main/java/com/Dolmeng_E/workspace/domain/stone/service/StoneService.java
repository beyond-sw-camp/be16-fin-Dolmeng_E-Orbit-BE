package com.Dolmeng_E.workspace.domain.stone.service;

import com.Dolmeng_E.workspace.common.service.AccessCheckService;
import com.Dolmeng_E.workspace.domain.project.entity.Project;
import com.Dolmeng_E.workspace.domain.project.repository.ProjectRepository;
import com.Dolmeng_E.workspace.domain.project.service.ProjectService;
import com.Dolmeng_E.workspace.domain.stone.dto.TopStoneCreateDto;
import com.Dolmeng_E.workspace.domain.stone.entity.Stone;
import com.Dolmeng_E.workspace.domain.stone.entity.StoneStatus;
import com.Dolmeng_E.workspace.domain.stone.repository.ChildStoneListRepository;
import com.Dolmeng_E.workspace.domain.stone.repository.StoneRepository;
import com.Dolmeng_E.workspace.domain.stone.repository.TaskRepository;
import com.Dolmeng_E.workspace.domain.workspace.entity.Workspace;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import com.Dolmeng_E.workspace.domain.workspace.repository.WorkspaceParticipantRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class StoneService {
    private final ChildStoneListRepository childStoneListRepository;
    private final TaskRepository taskRepository;
    private final StoneRepository stoneRepository;
    private final AccessCheckService accessCheckService;
    private final WorkspaceParticipantRepository workspaceParticipantRepository;
    private final ProjectRepository projectRepository;

    // 최상위 스톤 생성(프로젝트 생성 시 자동 생성)
    public String createStone(TopStoneCreateDto dto) {

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

        // 3. 스톤생성 후 ID 리턴
        return stoneRepository.save( Stone.builder()
                .stoneName(dto.getStoneName())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .project(project)
                .stoneParticipant(participant)
                .chatCreation(dto.getChatCreation() != null ? dto.getChatCreation() : false)
                .taskCreation(dto.getTaskCreation() != null ? dto.getTaskCreation() : false)
                .milestone(BigDecimal.ZERO) // 최초 마일스톤은 0퍼센트
                .status(StoneStatus.PROGRESS) // 최초 상태는 진행중으로 세팅
                .build()
        ).getId();

    }

    // 일반 스톤 생성

    // 스톤 수정

    // 스톤 삭제

    // 스톤 목록? 스톤들 뿌리처럼 보이는 거

    // 스톤 상세 정보 조회

    // 태스크 생성

    // 태스크 수정

    // 태스크 삭제

    // 태스크 완료 처리

    // 마일스톤 진행률 변경

    // To-Do: 다 하면 프로젝트 쪽 로직 완성

}
