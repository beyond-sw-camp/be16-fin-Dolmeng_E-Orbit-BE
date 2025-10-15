package com.Dolmeng_E.workspace.domain.stone.service;

import com.Dolmeng_E.workspace.common.service.AccessCheckService;
import com.Dolmeng_E.workspace.domain.project.entity.Project;
import com.Dolmeng_E.workspace.domain.project.entity.StoneParticipant;
import com.Dolmeng_E.workspace.domain.project.repository.ProjectRepository;
import com.Dolmeng_E.workspace.domain.project.repository.StoneParticipantRepository;
import com.Dolmeng_E.workspace.domain.project.service.ProjectService;
import com.Dolmeng_E.workspace.domain.stone.dto.StoneCreateDto;
import com.Dolmeng_E.workspace.domain.stone.dto.TopStoneCreateDto;
import com.Dolmeng_E.workspace.domain.stone.entity.ChildStoneList;
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
import java.util.ArrayList;
import java.util.List;
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
    private final StoneParticipantRepository stoneParticipantRepository;

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

        // 3. 스톤생성 후 ID 리턴
        return stoneRepository.save( Stone.builder()
                .stoneName(dto.getStoneName())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .project(project)
                .stoneParticipant(participant)
                .chatCreation(dto.getChatCreation() != null ? dto.getChatCreation() : false)
                .taskCreation(false) // 최상위 스톤은 태스크 x
                .milestone(dto.getMilestone() != null ? dto.getMilestone() : BigDecimal.ZERO) // 최초 마일스톤은 0퍼센트
                .status(StoneStatus.PROGRESS) // 최초 상태는 진행중으로 세팅
                .build()
        ).getId();

    }

    // 일반 스톤 생성
    public String createStone(String userId, StoneCreateDto dto) {

        // 1. 참여자 검증
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(dto.getWorkspaceId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 2. 프로젝트 조회
        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다."));

        // 3. 스톤 관련 권한 검증(프로젝트 담당자도 스톤 생성 가능하게)
        // Memo: 프로젝트와 스톤의 권한을 합치는 것도 고려할 부분(다만, 그렇게 되면 스톤 생성 권한이 있는 사람도 프로젝트 수정이 됨)
        if (!project.getWorkspaceParticipant().getId().equals(participant.getId())) {
            accessCheckService.validateAccess(participant, "ws_acc_list_3");
        }

        // 4. 상위 스톤 객체 조회
        Stone parentStone = stoneRepository.findById(dto.getParentStoneId())
                .orElseThrow(()->new EntityNotFoundException("상위스톤이 존재하지 않습니다."));

        // 5. 상위 스톤 ID를 넣어 스톤 객체 생성
        Stone childStone = stoneRepository.save(
                Stone.builder()
                        .stoneName(dto.getStoneName())
                        .startTime(dto.getStartTime())
                        .endTime(dto.getEndTime())
                        .project(project)
                        .stoneParticipant(participant)
                        .chatCreation(dto.getChatCreation() != null ? dto.getChatCreation() : false)
                        .parentStoneId(parentStone.getId()) //일반 스톤 생성의 경우 부모스톤 추가
                        .taskCreation(true) // 무조건 태스크 허용 - default도 ture
                        .milestone(dto.getMilestone() != null ? dto.getMilestone() : BigDecimal.ZERO) // 최초 마일스톤은 0퍼센트, NPE방지
                        .status(StoneStatus.PROGRESS) // 최초 상태는 진행중으로 세팅
                        .build()
        );

        // 6. 상위스톤의 자식스톤 리스트 생성
        childStoneListRepository.save(
                ChildStoneList.builder()
                        .stone(parentStone) // 부모스톤
                        .childStone(childStone) // 자식스톤
                        .build()
        );

        // 7. dto에 스톤 참여자 목록이 있다면 저장
        if(dto.getParticipantIds() != null && !dto.getParticipantIds().isEmpty()) {
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
        return childStone.getId();
    }

    // 스톤 참여자 추가

    // 스톤 안보임 설정(프로젝트 캘린더 조회용 API)

    // 내 스톤 목록 조회

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
