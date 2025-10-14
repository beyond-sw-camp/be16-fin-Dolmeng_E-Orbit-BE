package com.Dolmeng_E.workspace.domain.project.service;

import com.Dolmeng_E.workspace.domain.access_group.entity.AccessDetail;
import com.Dolmeng_E.workspace.domain.access_group.entity.AccessGroup;
import com.Dolmeng_E.workspace.domain.access_group.repository.AccessDetailRepository;
import com.Dolmeng_E.workspace.domain.access_group.repository.AccessGroupRepository;
import com.Dolmeng_E.workspace.domain.project.dto.ProjectCreateDto;
import com.Dolmeng_E.workspace.domain.project.dto.ProjectDeleteDto;
import com.Dolmeng_E.workspace.domain.project.dto.ProjectListDto;
import com.Dolmeng_E.workspace.domain.project.dto.ProjectModifyDto;
import com.Dolmeng_E.workspace.domain.project.entity.Project;
import com.Dolmeng_E.workspace.domain.project.entity.ProjectStatus;
import com.Dolmeng_E.workspace.domain.project.repository.ProjectCalendarRepository;
import com.Dolmeng_E.workspace.domain.project.repository.ProjectParticipantRepository;
import com.Dolmeng_E.workspace.domain.project.repository.ProjectRepository;
import com.Dolmeng_E.workspace.domain.workspace.entity.Workspace;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import com.Dolmeng_E.workspace.domain.workspace.repository.WorkspaceParticipantRepository;
import com.Dolmeng_E.workspace.domain.workspace.repository.WorkspaceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectCalendarRepository projectCalendarRepository;
    private final ProjectParticipantRepository projectParticipantRepository;
    private final WorkspaceParticipantRepository workspaceParticipantRepository;
    private final AccessDetailRepository accessDetailRepository;
    private final AccessGroupRepository accessGroupRepository;
    private final WorkspaceRepository workspaceRepository;

// 프로젝트 생성

    public String createProject(String userId, ProjectCreateDto dto) {
        // 1. 참여자 검증
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(dto.getWorkspaceId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 2. 권한 검증 (공통 메서드)
        // 커스터마이징 필요한 부분 ex. 프로젝트 관련 -> ws_acc_list_2 , 스톤 관련 -> ws_acc_list_3 등등
        validateAccess(participant, "ws_acc_list_2");

        // 3. 스톤 생성



        // 4. 워크스페이스 담당자 객체 생성
        WorkspaceParticipant projectManager = workspaceParticipantRepository.findById(dto.getProjectManagerId())
                .orElseThrow(()->new EntityNotFoundException("회원 정보 없습니다."));

        if (!projectManager.getWorkspace().getId().equals(dto.getWorkspaceId())) {
            throw new IllegalArgumentException("담당자는 해당 워크스페이스 소속이어야 합니다.");
        }

        // 5. 프로젝트 생성
        Workspace workspace = workspaceRepository.findById(dto.getWorkspaceId())
                .orElseThrow(()->new EntityNotFoundException("워크스페이스 정보를 찾을 수 없습니다."));
        Project project = dto.toEntity(workspace,projectManager);
        projectRepository.save(project);

        //To-Do: 만약 boolean이 true면 채팅방, 스톤 생성 로직 추가해야 함.
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

        // 3. 권한 검증: 담당자 or 권한그룹
        if (!project.getWorkspaceParticipant().getId().equals(participant.getId())) {
            validateAccess(participant, "ws_acc_list_2");
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
        List<Project> projectlist = projectRepository.findAllByWorkspaceIdAndIsDeletedFalseAndProjectStatusNot(workspaceId, ProjectStatus.PROGRESS);
        List<ProjectListDto> projectListDtoList = projectlist.stream().map(ProjectListDto::fromEntity).toList();
        return projectListDtoList;
    }

// 프로젝트 상세 조회(To-Do: 스톤, 태스트 생성 완료 후 프로젝트 구조 조회 API 구현 필요)


// 프로젝트 삭제
    public void deleteProject(String userId, ProjectDeleteDto dto) {
        // 1. 참여자 검증
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(dto.getWorkspaceId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 2. 프로젝트 조회
        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new EntityNotFoundException("프로젝트를 찾을 수 없습니다."));

        // 3. 권한 검증: 담당자 or 권한그룹
        if (!project.getWorkspaceParticipant().getId().equals(participant.getId())) {
            validateAccess(participant, "ws_acc_list_2");
        }

        // 4. 삭제
        project.deleteProject();
    }


    // 공통 서비스 로직: 권한 체크
    public void validateAccess(WorkspaceParticipant participant, String accessListId) {
        String accessGroupId = participant.getAccessGroup().getId();
        AccessGroup accessGroup = accessGroupRepository.findById(accessGroupId)
                .orElseThrow(() -> new EntityNotFoundException("권한그룹이 존재하지 않습니다."));

        AccessDetail accessDetail = accessDetailRepository
                .findByAccessGroupAndAccessListId(accessGroup, accessListId)
                .orElseThrow(() -> new EntityNotFoundException("권한 상세정보가 없습니다."));

        if (!accessDetail.getIsAccess()) {
            throw new IllegalArgumentException("해당 작업에 대한 권한이 없습니다.");
        }
    }


}
