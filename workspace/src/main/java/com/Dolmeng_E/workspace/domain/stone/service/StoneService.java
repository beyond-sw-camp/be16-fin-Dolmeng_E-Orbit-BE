package com.Dolmeng_E.workspace.domain.stone.service;

import com.Dolmeng_E.workspace.common.controller.DriveServiceClient;
import com.Dolmeng_E.workspace.common.controller.SearchServiceClient;
import com.Dolmeng_E.workspace.common.dto.*;
import com.Dolmeng_E.workspace.common.service.AccessCheckService;
import com.Dolmeng_E.workspace.common.service.ChatFeign;
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
import com.Dolmeng_E.workspace.domain.task.dto.TaskKafkaUpdateDto;
import com.Dolmeng_E.workspace.domain.task.entity.Task;
import com.Dolmeng_E.workspace.domain.task.repository.TaskRepository;
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
import com.Dolmeng_E.workspace.domain.stone.dto.TaskResDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.LocalDateTime.now;

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
    private final ChatFeign chatFeign;
    private final DriveServiceClient driveServiceClient;
    private final SearchServiceClient searchServiceClient;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

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
        Stone childStone = stoneRepository.saveAndFlush(
                Stone.builder()
                        .stoneName(dto.getStoneName())
                        .startTime(dto.getStartTime())
                        .endTime(dto.getEndTime())
                        .project(project)
                        .stoneDescribe(dto.getStoneDescribe())
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

            // 스톤 참여자에게 알림 발송
            List<UUID> participantList = participantEntities.stream().map(stone->stone.getWorkspaceParticipant()
                    .getUserId()).toList();
            // 테스트 코드
            List<UUID> userIdList = new ArrayList<>(participantList);

            NotificationCreateReqDto notificationCreateReqDto = NotificationCreateReqDto.builder()
                    .title("[" + workspace.getWorkspaceName() + "]" + "스톤 참여자 추가")
                    .content(childStone.getStoneName() + " 스톤 참여자에 추가되었습니다! 🎉")
                    .userIdList(userIdList)
                    .type("STONE_MESSAGE")
                    .sendAt(null)
                    .projectId(project.getId())
                    .workspaceId(workspace.getId())
                    .stoneId(childStone.getId())
                    .build();
        }

        // 12. 프로젝트/마일스톤 반영
        project.incrementStoneCount();
        projectRepository.save(project);
        milestoneCalculator.updateStoneAndParents(parentStone);

        // 13. 채팅방 생성 및 초대 (chatCreation이 true인 경우)
        if (Boolean.TRUE.equals(childStone.getChatCreation())) {

            // 1️. 채팅방 생성
            ChatCreateReqDto chatCreateReqDto = ChatCreateReqDto.builder()
                    .workspaceId(workspace.getId())
                    .projectId(project.getId())
                    .stoneId(childStone.getId())
                    .roomName(childStone.getStoneName()) // 스톤명 기반 채팅방명
                    .build();

            chatFeign.createChatRoom(chatCreateReqDto);

            // 2. 채팅방에 초대할 인원 구성
            List<UUID> userIdList = new ArrayList<>();

            // 스톤 참여자
            if (dto.getParticipantIds() != null && !dto.getParticipantIds().isEmpty()) {
                userIdList.addAll(dto.getParticipantIds());
            }

            // 스톤 담당자(생성자) 포함
            userIdList.add(participant.getUserId());

            // 중복 제거
            List<UUID> distinctUserList = userIdList.stream().distinct().toList();

            // 3. 초대 요청
            ChatInviteReqDto chatInviteReqDto = ChatInviteReqDto.builder()
                    .workspaceId(workspace.getId())
                    .projectId(project.getId())
                    .stoneId(childStone.getId())
                    .userIdList(distinctUserList)
                    .build();

            chatFeign.inviteChatParticipants(chatInviteReqDto);
        }

        // kafka 메시지 발행
        Set<String> viewableUserIds = new HashSet<>();
        List<ProjectParticipant> projectParticipants = projectParticipantRepository.findAllByProject(childStone.getProject());
        for(ProjectParticipant pp : projectParticipants) {
            viewableUserIds.add(pp.getWorkspaceParticipant().getUserId().toString());
        }
        viewableUserIds.add(workspaceParticipantRepository.findByWorkspaceIdAndWorkspaceRole(workspace.getId(), WorkspaceRole.ADMIN).getUserId().toString());
        List<StoneParticipant> stoneParticipants = stoneParticipantRepository.findAllByStone(childStone);
        List<StoneKafkaSaveDto.EventPayload.ParticipantInfo> participantInfos = new ArrayList<>();
        for(StoneParticipant sp : stoneParticipants) {
            participantInfos.add(StoneKafkaSaveDto.EventPayload.ParticipantInfo.builder()
                    .id(sp.getWorkspaceParticipant().getUserId().toString())
                    .build());
        }
        StoneKafkaSaveDto stoneKafkaSaveDto = StoneKafkaSaveDto.builder()
                .eventType("STONE_CREATED")
                .eventPayload(StoneKafkaSaveDto.EventPayload.builder()
                        .id(childStone.getId())
                        .name(childStone.getStoneName())
                        .viewableUserIds(viewableUserIds)
                        .description(childStone.getStoneDescribe())
                        .participants(participantInfos)
                        .endDate(childStone.getEndTime())
                        .manager(childStone.getStoneManager().getUserId().toString())
                        .rootType("PROJECT")
                        .projectId(childStone.getProject().getId())
                        .status(childStone.getStatus().toString())
                        .workspaceId(childStone.getProject().getWorkspace().getId())
                        .build())
                .build();
        try {
            String message = objectMapper.writeValueAsString(stoneKafkaSaveDto);
            kafkaTemplate.send("stone-topic", message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Kafka 메시지 직렬화 실패", e);
        }

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

            List<WorkspaceParticipant> toCheck = existingParticipants.stream()
                    .map(StoneParticipant::getWorkspaceParticipant)
                    .toList();

            stoneParticipantRepository.deleteAll(existingParticipants);

            for (WorkspaceParticipant wp : toCheck) {
                boolean stillInOtherStones = stoneParticipantRepository
                        .existsByStone_ProjectAndWorkspaceParticipant(project, wp);
                if (!stillInOtherStones) {
                    projectParticipantRepository.findByProjectAndWorkspaceParticipant(project, wp)
                            .ifPresent(projectParticipantRepository::delete);
                }
            }
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

            List<WorkspaceParticipant> deletedWps = toDelete.stream()
                    .map(StoneParticipant::getWorkspaceParticipant)
                    .toList();

            stoneParticipantRepository.deleteAll(toDelete);

            for (WorkspaceParticipant wp : deletedWps) {
                boolean stillInOtherStones = stoneParticipantRepository
                        .existsByStone_ProjectAndWorkspaceParticipant(project, wp);
                if (!stillInOtherStones) {
                    projectParticipantRepository.deleteByProjectAndWorkspaceParticipant(project, wp);
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

        // 알림용 참여자 ID 리스트 조립
        List<UUID> participantIdList = newParticipants.stream()
                .map(sp -> sp.getWorkspaceParticipant().getUserId())
                .distinct()
                .toList();

        // 스톤 참여자에게 알림 발송 (테스트 코드 예시)
        List<UUID> userIdList = new ArrayList<>(participantIdList);

        NotificationCreateReqDto notificationCreateReqDto = NotificationCreateReqDto.builder()
                .title("[" + workspace.getWorkspaceName() + "]" + "스톤 참여자 등록")
                .content(stone.getStoneName() +  " 스톤 참여자로 등록되었습니다! 🎉")
                .userIdList(userIdList)
                .type("STONE_MESSAGE")
                .sendAt(null)
                .stoneId(stone.getId())
                .projectId(project.getId())
                .workspaceId(workspace.getId())
                .build();

        // 추가 : 채팅방 인원 추가 (채팅방 생성된 스톤만)
        if (Boolean.TRUE.equals(stone.getChatCreation()) && !newParticipants.isEmpty()) {

            UUID managerId = stone.getStoneManager().getUserId();

            // 초대 대상: 신규 스톤참여자들 중 "매니저 제외"
            List<UUID> inviteIds = newParticipants.stream()
                    .map(sp -> sp.getWorkspaceParticipant().getUserId())
                    .filter(id -> !id.equals(managerId))   // <-- 매니저 필터링
                    .distinct()
                    .toList();

            if (!inviteIds.isEmpty()) {
                ChatInviteReqDto chatInviteReqDto = ChatInviteReqDto.builder()
                        .workspaceId(workspace.getId())
                        .projectId(project.getId())
                        .stoneId(stone.getId())
                        .userIdList(inviteIds)
                        .build();

                chatFeign.inviteChatParticipants(chatInviteReqDto);
            }
        }

        // 삭제 대상
        Set<UUID> deleteIds = existingUserIds.stream()
                .filter(id -> !newUserIds.contains(id))
                .collect(Collectors.toSet());
        // kafka 메시지 발행
        StoneKafkaViewableUpdateDto stoneKafkaViewableUpdateDto = StoneKafkaViewableUpdateDto.builder()
                .eventType("STONE_PARTICIPANT_UPDATE")
                .eventPayload(StoneKafkaViewableUpdateDto.EventPayload.builder()
                        .id(stone.getId())
                        .type("DELETE")
                        .userIds(deleteIds)
                        .projectId(stone.getProject().getId())
                        .build())
                .build();
        try {
            String message = objectMapper.writeValueAsString(stoneKafkaViewableUpdateDto);
            kafkaTemplate.send("update-viewable-topic", message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Kafka 메시지 직렬화 실패", e);
        }


        // 추가 대상
        Set<UUID> createIds = newUserIds.stream()
                .filter(id -> !existingUserIds.contains(id))
                .collect(Collectors.toSet());

        // kafka 메시지 발행
        StoneKafkaViewableUpdateDto stoneKafkaViewableUpdateDto1 = StoneKafkaViewableUpdateDto.builder()
                .eventType("STONE_PARTICIPANT_UPDATE")
                .eventPayload(StoneKafkaViewableUpdateDto.EventPayload.builder()
                        .id(stone.getId())
                        .type("CREATE")
                        .userIds(createIds)
                        .projectId(stone.getProject().getId())
                        .build())
                .build();
        try {
            String message = objectMapper.writeValueAsString(stoneKafkaViewableUpdateDto1);
            kafkaTemplate.send("update-viewable-topic", message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Kafka 메시지 직렬화 실패", e);
        }
    }


    // 스톤 참여자 리스트 삭제 (선택 삭제)
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

        // 5. 스톤 참여자 삭제 (UUID 기반)
        if (dto.getStoneParticipantList() != null && !dto.getStoneParticipantList().isEmpty()) {
            List<StoneParticipant> toDeleteStoneParticipants = new ArrayList<>();
            List<WorkspaceParticipant> deletedWps = new ArrayList<>();

            for (UUID userUuid : dto.getStoneParticipantList()) {
                WorkspaceParticipant wp = workspaceParticipantRepository
                        .findByWorkspaceIdAndUserId(project.getWorkspace().getId(), userUuid)
                        .orElseThrow(() -> new EntityNotFoundException("참여자 정보를 찾을 수 없습니다."));

                StoneParticipant stoneParticipant = stoneParticipantRepository
                        .findByStoneAndWorkspaceParticipant(stone, wp)
                        .orElseThrow(() -> new EntityNotFoundException("스톤 참여자 정보를 찾을 수 없습니다."));

                toDeleteStoneParticipants.add(stoneParticipant);
                deletedWps.add(wp);
            }

            stoneParticipantRepository.deleteAll(toDeleteStoneParticipants);

            for (WorkspaceParticipant wp : deletedWps) {
                boolean stillInOtherStones = stoneParticipantRepository
                        .existsByStone_ProjectAndWorkspaceParticipant(project, wp);
                if (!stillInOtherStones) {
                    projectParticipantRepository.findByProjectAndWorkspaceParticipant(project, wp)
                            .ifPresent(projectParticipantRepository::delete);
                }
            }
        }
    }


    // 스톤 참여자 전체 삭제 (해당 스톤만)
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

        // 5. 스톤 참여자 전체 삭제 준비
        List<StoneParticipant> participants = stoneParticipantRepository.findAllByStone(stone);
        if (!participants.isEmpty()) {
            List<WorkspaceParticipant> deletedWps = participants.stream()
                    .map(StoneParticipant::getWorkspaceParticipant)
                    .toList();

            stoneParticipantRepository.deleteAll(participants);

            for (WorkspaceParticipant wp : deletedWps) {
                boolean stillExists = stoneParticipantRepository
                        .existsByStone_ProjectAndWorkspaceParticipant(project, wp);

                if (!stillExists) {
                    projectParticipantRepository.findByProjectAndWorkspaceParticipant(project, wp)
                            .ifPresent(projectParticipantRepository::delete);
                }
            }
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
        if (dto.getStoneDescribe() != null) stone.setStoneDescribe(dto.getStoneDescribe());

        // 7. 채팅방 생성 여부 방어 로직
        if (dto.getChatCreation() != null) {
            boolean prev = stone.getChatCreation();  // 현재 DB 저장 상태
            boolean next = dto.getChatCreation();    // 수정 요청 값

            // 이미 true인데 false로 바꾸려 하면 막기
            if (!prev && next) {
                stone.setChatCreation(true);

                // 스톤 담당자 포함
                List<UUID> userIdList = new ArrayList<>(
                        stoneParticipantRepository.findAllByStone(stone)
                                .stream()
                                .map(sp -> sp.getWorkspaceParticipant().getUserId())
                                .toList()
                );
                userIdList.add(stone.getStoneManager().getUserId()); // 담당자 추가

                List<UUID> distinctUserList = userIdList.stream().distinct().toList();

                // 1. 채팅방 생성 (roomName은 스톤 이름 기반으로)
                ChatCreateReqDto createDto = ChatCreateReqDto.builder()
                        .workspaceId(workspace.getId())
                        .projectId(project.getId())
                        .stoneId(stone.getId())
                        .roomName(stone.getStoneName())
                        .userIdList(distinctUserList)
                        .build();

                chatFeign.createChatRoom(createDto);  // 생성 호출

                // 2. (선택) 이미 참여자 목록이 있다면, 이후 초대 로직도 가능
                // chatFeign.inviteChatParticipants(chatInviteReqDto);
            }

        }
        if (dto.getEndTime() != null) {
            stone.setEndTime(dto.getEndTime());
        }

        // 8. 수정된 스톤 저장
        stoneRepository.save(stone);
        milestoneCalculator.updateStoneAndParents(stone);

        // kafka 메시지 발행
        StoneKafkaUpdateDto stoneKafkaUpdateDto = StoneKafkaUpdateDto.builder()
                .eventType("STONE_UPDATED")
                .eventPayload(StoneKafkaUpdateDto.EventPayload.builder()
                        .id(stone.getId())
                        .name(stone.getStoneName())
                        .description(stone.getStoneDescribe())
                        .endDate(stone.getEndTime())
                        .build())
                .build();
        try {
            // 3. DTO를 JSON 문자열로 변환
            String message = objectMapper.writeValueAsString(stoneKafkaUpdateDto);

            // 4. Kafka 토픽으로 이벤트 발행
            kafkaTemplate.send("stone-topic", message);

        } catch (JsonProcessingException e) {
            // 예외 처리 (심각한 경우 트랜잭션 롤백 고려)
            throw new RuntimeException("Kafka 메시지 직렬화 실패", e);
        }

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

        // kafka 메시지 발행
        StoneKafkaUpdateDto stoneKafkaUpdateDto = StoneKafkaUpdateDto.builder()
                .eventType("STONE_UPDATED")
                .eventPayload(StoneKafkaUpdateDto.EventPayload.builder()
                        .id(stone.getId())
                        .manager(stone.getStoneManager().getUserId().toString())
                        .build())
                .build();
        try {
            // 3. DTO를 JSON 문자열로 변환
            String message = objectMapper.writeValueAsString(stoneKafkaUpdateDto);

            // 4. Kafka 토픽으로 이벤트 발행
            kafkaTemplate.send("stone-topic", message);

        } catch (JsonProcessingException e) {
            // 예외 처리 (심각한 경우 트랜잭션 롤백 고려)
            throw new RuntimeException("Kafka 메시지 직렬화 실패", e);
        }

        // 8. 변경된 스톤 저장
        stoneRepository.save(stone);

        //todo chatCreation이 true면 채팅방 생성 로직 추가해야함. 수정의 경우, false -> true가 돼도 기존에 채팅방이 생성되어있으면 생성안되게

    }

    // 스톤 삭제
    public void deleteStone(String userId, String stoneId) {
        driveServiceClient.deleteAll("STONE", stoneId);
        searchServiceClient.deleteAll("STONE", stoneId);

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
            boolean stillExists = stoneParticipantRepository.existsByStone_ProjectAndWorkspaceParticipant(project, wp);
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

        // 스톤 완료시간 추가
        LocalDateTime __now = now();
        stone.setStoneCompletedDay(__now);

        // 상위스톤 담당자에게 알림 발송 (예시)
        List<UUID> userIdList = new ArrayList<>();
        if (stone.getParentStoneId() != null) {
            Stone topStone = stoneRepository.findById(stone.getParentStoneId())
                    .orElseThrow(() -> new EntityNotFoundException("상위 스톤이 없습니다."));
            userIdList.add(topStone.getStoneManager().getUserId());
        }

        NotificationCreateReqDto notificationCreateReqDto = NotificationCreateReqDto.builder()
                .title("[" + workspace.getWorkspaceName() + "]" + "하위스톤 완료")
                .content(stone.getStoneName() + " 스톤이 완료되었습니다! 🎉")
                .userIdList(userIdList)
                .type("STONE_MESSAGE")
                .sendAt(null)
                .stoneId(stone.getId())
                .projectId(project.getId())
                .workspaceId(workspace.getId())
                .build();

        // kafka 메시지 발행
        StoneKafkaUpdateDto stoneKafkaUpdateDto = StoneKafkaUpdateDto.builder()
                .eventType("STONE_UPDATED")
                .eventPayload(StoneKafkaUpdateDto.EventPayload.builder()
                        .id(stone.getId())
                        .status(stone.getStatus().toString())
                        .build())
                .build();
        try {
            // 3. DTO를 JSON 문자열로 변환
            String message = objectMapper.writeValueAsString(stoneKafkaUpdateDto);

            // 4. Kafka 토픽으로 이벤트 발행
            kafkaTemplate.send("stone-topic", message);

        } catch (JsonProcessingException e) {
            // 예외 처리 (심각한 경우 트랜잭션 롤백 고려)
            throw new RuntimeException("Kafka 메시지 직렬화 실패", e);
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

    // 워크스페이스 ID로 스톤 전체 조회
    public List<StoneListResDto> getStonesByWorkspace(String workspaceId) {
        List<Stone> stones = stoneRepository.findAllByWorkspaceId(workspaceId);
        return stones.stream()
                .map(StoneListResDto::fromEntity)
                .toList();
    }

    // 스톤 ID로 테스크 목록 조회
    public List<SubTaskResDto> getSubTasksByStone(String stoneId){
        Stone stone = stoneRepository.findById(stoneId).orElseThrow(()->new EntityNotFoundException("존재하지 않는 스톤입니다."));
        List<Task> tasks = taskRepository.findAllByStone(stone);
        return tasks.stream()
                .map(SubTaskResDto::new)
                .toList();
    }
}
