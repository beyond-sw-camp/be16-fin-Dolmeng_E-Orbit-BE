package com.Dolmeng_E.workspace.domain.project.service;

import com.Dolmeng_E.workspace.common.service.AccessCheckService;
import com.Dolmeng_E.workspace.domain.access_group.repository.AccessDetailRepository;
import com.Dolmeng_E.workspace.domain.access_group.repository.AccessGroupRepository;
import com.Dolmeng_E.workspace.domain.project.dto.ProjectCreateDto;
import com.Dolmeng_E.workspace.domain.project.dto.ProjectListDto;
import com.Dolmeng_E.workspace.domain.project.dto.ProjectModifyDto;
import com.Dolmeng_E.workspace.domain.project.dto.ProjectProgressResDto;
import com.Dolmeng_E.workspace.domain.project.entity.Project;
import com.Dolmeng_E.workspace.domain.project.entity.ProjectParticipant;
import com.Dolmeng_E.workspace.domain.project.entity.ProjectStatus;
import com.Dolmeng_E.workspace.domain.stone.entity.Stone;
import com.Dolmeng_E.workspace.domain.stone.entity.StoneParticipant;
import com.Dolmeng_E.workspace.domain.stone.repository.StoneParticipantRepository;
import com.Dolmeng_E.workspace.domain.project.repository.ProjectParticipantRepository;
import com.Dolmeng_E.workspace.domain.project.repository.ProjectRepository;
import com.Dolmeng_E.workspace.domain.stone.dto.TopStoneCreateDto;
import com.Dolmeng_E.workspace.domain.stone.repository.StoneRepository;
import com.Dolmeng_E.workspace.domain.stone.service.StoneService;
import com.Dolmeng_E.workspace.domain.workspace.entity.Workspace;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceRole;
import com.Dolmeng_E.workspace.domain.workspace.repository.WorkspaceParticipantRepository;
import com.Dolmeng_E.workspace.domain.workspace.repository.WorkspaceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final StoneParticipantRepository projectCalendarRepository;
    private final ProjectParticipantRepository projectParticipantRepository;
    private final WorkspaceParticipantRepository workspaceParticipantRepository;
    private final AccessDetailRepository accessDetailRepository;
    private final AccessGroupRepository accessGroupRepository;
    private final WorkspaceRepository workspaceRepository;
    private final AccessCheckService accessCheckService;
    private final StoneService stoneService;
    private final StoneRepository stoneRepository;
    private final StoneParticipantRepository stoneParticipantRepository;

// 프로젝트 생성

    public String createProject(String userId, ProjectCreateDto dto) {
        // 1. 참여자 검증
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(dto.getWorkspaceId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 2. 권한 검증 (공통 메서드)
        // 커스터마이징 필요한 부분 ex. 프로젝트 관련 -> ws_acc_list_2 , 스톤 관련 -> ws_acc_list_3 등등
        if (!participant.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            accessCheckService.validateAccess(participant, "ws_acc_list_2");
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
        if (!participant.getWorkspaceRole().equals(WorkspaceRole.ADMIN)) {
            if (!project.getWorkspaceParticipant().getId().equals(participant.getId())) {
                accessCheckService.validateAccess(participant, "ws_acc_list_2");
            }
        }

        // 4. 담당자 변경 처리 (선택)
        if (dto.getProjectManagerId() != null) {
            WorkspaceParticipant newManager = workspaceParticipantRepository.findById(dto.getProjectManagerId())
                    .orElseThrow(() -> new EntityNotFoundException("변경할 담당자 정보를 찾을 수 없습니다."));

            if (!newManager.getWorkspace().getId().equals(dto.getWorkspaceId())) {
                throw new IllegalArgumentException("담당자는 해당 워크스페이스 소속이어야 합니다.");
            }

            project.changeManager(newManager);
        }

        project.update(dto);
        return project.getId();
    }

// 프로젝트 목록 조회(사이드 바 프로젝트 목록)
    public List<ProjectListDto> getProjectList(String userId, String workspaceId) {
        // 1. 참여자 검증
        WorkspaceParticipant workspaceParticipant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspaceId, UUID.fromString(userId))
                .orElseThrow(()->new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 2. dto 조립, 진행중,완료인 프로젝트만 보기
        List<Project> projectlist = projectRepository.findAllByWorkspaceIdAndIsDeleteFalseAndProjectStatusNot(workspaceId, ProjectStatus.STORAGE);
        List<ProjectListDto> projectListDtoList = projectlist.stream().map(ProjectListDto::fromEntity).toList();
        return projectListDtoList;
    }


// 프로젝트 삭제
    public void deleteProject(String userId, String projectId) {

        // 1. 프로젝트 조회
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다."));
        Workspace workspace = project.getWorkspace();
        // 2. 참여자 검증
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 3. 권한 검증: 관리자 or 프로젝트 담당자 or 권한그룹
        if (participant.getWorkspaceRole() != WorkspaceRole.ADMIN &&
                !project.getWorkspaceParticipant().getId().equals(participant.getId())) {
            accessCheckService.validateAccess(participant, "ws_acc_list_2");
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

            // 5-2. 스톤 참여자 하드삭제
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


// 프로젝트가 프로젝트 캘린더에 노출 여부 설정(프로젝트 캘린더 조회용 API)

// 내 프로젝트 목록 조회





}
