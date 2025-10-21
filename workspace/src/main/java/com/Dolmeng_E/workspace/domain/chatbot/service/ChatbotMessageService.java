package com.Dolmeng_E.workspace.domain.chatbot.service;

import com.Dolmeng_E.workspace.common.service.ChatFeign;
import com.Dolmeng_E.workspace.common.service.RestTemplateClient;
import com.Dolmeng_E.workspace.domain.chatbot.dto.*;
import com.Dolmeng_E.workspace.domain.chatbot.entity.ChatbotMessage;
import com.Dolmeng_E.workspace.domain.chatbot.entity.ChatbotMessageType;
import com.Dolmeng_E.workspace.domain.chatbot.repository.ChatbotMessageRepository;
import com.Dolmeng_E.workspace.domain.project.entity.Project;
import com.Dolmeng_E.workspace.domain.project.entity.ProjectParticipant;
import com.Dolmeng_E.workspace.domain.project.repository.ProjectParticipantRepository;
import com.Dolmeng_E.workspace.domain.project.repository.ProjectRepository;
import com.Dolmeng_E.workspace.domain.task.entity.Task;
import com.Dolmeng_E.workspace.domain.task.repository.TaskRepository;
import com.Dolmeng_E.workspace.domain.workspace.entity.WorkspaceParticipant;
import com.Dolmeng_E.workspace.domain.workspace.repository.WorkspaceParticipantRepository;
import com.example.modulecommon.dto.CommonSuccessDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatbotMessageService {
    static final String AGENT_URL = "http://localhost:5678/webhook/chatbot-agent";
    static final String AGENT_URL_CHAT = "http://localhost:5678/webhook-test/chatbot-agent/chat-summary";

    private final ChatbotMessageRepository chatbotMessageRepository;
    private final WorkspaceParticipantRepository workspaceParticipantRepository;
    private final RestTemplateClient restTemplateClient;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final ChatFeign chatFeign;
    private final ProjectParticipantRepository projectParticipantRepository;
    private final SemanticMemoryService semanticMemoryService;

    // 사용자가 챗봇에게 메시지 전송
    public N8nResDto sendMessage(String userId, ChatbotMessageUserReqDto reqDto) {
        // 워크스페이스 참여자 검증
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(reqDto.getWorkspaceId(), UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 사용자가 보낸 메시지 저장
        ChatbotMessage chatbotUserMessage = ChatbotMessage.builder()
                .content(reqDto.getContent())
                .type(ChatbotMessageType.USER)
                .workspaceParticipant(participant)
                .build();
        chatbotMessageRepository.save(chatbotUserMessage);

        // SemanticMemory 유사도 검사
        String semanticMessage = semanticMemoryService.findBotReplyByKeyFilter(reqDto.getWorkspaceId(), userId, reqDto.getContent());
        N8nResDto result;
        if(semanticMessage != null) {
            result = N8nResDto.builder()
                    .text(semanticMessage)
                    .isSave(false)
                    .build();

            // agent의 응답을 답장으로 저장
            ChatbotMessage chatbotBotMessage = ChatbotMessage.builder()
                    .content(result.getText())
                    .type(ChatbotMessageType.BOT)
                    .workspaceParticipant(participant)
                    .build();
            chatbotMessageRepository.save(chatbotBotMessage);
        } else {
            // 사용자 메시지를 agent에게 보내기전에 필요한 데이터 담기
            N8nAgentReqDto n8nAgentReqDto = N8nAgentReqDto.builder()
                    .workspaceId(reqDto.getWorkspaceId())
                    .content(reqDto.getContent())
                    .userId(userId)
                    .userName(participant.getUserName())
                    .today(String.valueOf(LocalDateTime.now()))
                    .build();

            // agent에게 요청 및 응답 받아오기
            ResponseEntity<N8nResDto> response =
                    restTemplateClient.post(AGENT_URL, n8nAgentReqDto, N8nResDto.class);
            result = response.getBody();

            // agent의 응답을 답장으로 저장
            ChatbotMessage chatbotBotMessage = ChatbotMessage.builder()
                    .content(result.getText())
                    .type(ChatbotMessageType.BOT)
                    .workspaceParticipant(participant)
                    .build();
            chatbotMessageRepository.save(chatbotBotMessage);
        }

        // isSave가 true일 때만 질문/답 Semantic에 저장
        if(result.getIsSave() != null && result.getIsSave()) {
            // redis-stack에 임베딩 저장
            // User
            UUID uuidKey = UUID.randomUUID();
            semanticMemoryService.saveToRedis(userId, reqDto.getWorkspaceId(), "USER", reqDto.getContent(), uuidKey);
            // Bot
            semanticMemoryService.saveToRedis(userId, reqDto.getWorkspaceId(), "BOT", result.getText(), uuidKey);
        }

        return result;
    }

    public N8nResDto sendRequestForChat(String userId, Long roomId) {
        ResponseEntity<CommonSuccessDto> response1 = chatFeign.getUnreadMessageListByRoom(roomId, userId);
        CommonSuccessDto body = response1.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        String unreadMessages = objectMapper.convertValue(body.getResult(), String.class);

        N8nAgentReqDto n8nAgentReqDto = N8nAgentReqDto.builder()
                .userId(userId)
                .content(unreadMessages)
                .build();

        // agent에게 요청 및 응답 받아오기
        ResponseEntity<N8nResDto> response2 =
                restTemplateClient.post(AGENT_URL_CHAT, n8nAgentReqDto, N8nResDto.class);
        N8nResDto result = response2.getBody();

        return result;
    }

    // 사용자와 챗봇의 대화 조회
    public List<ChatbotMessageListResDto> getUserMessageList(String userId, String workspaceId) {
        // 워크스페이스 참여자 검증
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(workspaceId, UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // participant의 챗봇과의 메시지 리스트 반환
        List<ChatbotMessage> chatbotMessageList = chatbotMessageRepository.findByWorkspaceParticipant(participant);

        return chatbotMessageList.stream().map(c -> ChatbotMessageListResDto.fromEntity(c)).toList();
    }

    // Agent전용
    // 프로젝트 요약을 위한 정보 제공
    public String getProjectInfo(ChatbotInfoReqDto reqDto) {
        Project project = null;

        // 프로젝트 가져와서 프로젝트 명, 목표, 설명, 진행도, 종료시간, 완료여부 반환
        if(reqDto.getProjectName().equals("current")) {
            // 워크스페이스 참여자 검증
            WorkspaceParticipant workspaceParticipant = workspaceParticipantRepository
                    .findByWorkspaceIdAndUserId(reqDto.getWorkspaceId(), UUID.fromString(reqDto.getUserId()))
                    .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

            project = projectParticipantRepository.findLatestProjectByParticipant(workspaceParticipant).orElseThrow(() -> new EntityNotFoundException("없는 프로젝트입니다."));
        } else {
            project = projectRepository.findByProjectName(reqDto.getProjectName()).orElseThrow(() -> new EntityNotFoundException("없는 프로젝트입니다."));
        }

        String projectInfo = "";
        projectInfo += "프로젝트명: " + project.getProjectName() + "\n";
        projectInfo += "프로젝트 목표: " + project.getProjectObjective() + "\n";
        projectInfo += "프로젝트 설명: " + project.getProjectDescription() + "\n";
        projectInfo += "프로젝트명 진행도: " + project.getMilestone() + "\n";
        projectInfo += "프로젝트명 기한 마감일: " + project.getEndTime() + "\n";
        projectInfo += "프로젝트명 상태: " + project.getProjectStatus() + "\n";

        return projectInfo;
    }

    // 사용자가 할당 된 모든 task리스트 반환
    public String getTaskList(ChatbotInfoReqDto reqDto) {
        LocalDateTime dtoEndTime = LocalDateTime.parse(reqDto.getEndTime());
        String taskList = "";

        // 워크스페이스 참여자 검증
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(reqDto.getWorkspaceId(), UUID.fromString(reqDto.getUserId()))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 참여자가 할당된 task 목록 가져오기, 필터링: 완료여부, 날짜
        List<Task> tasks = taskRepository.findUnfinishedTasksBeforeDate(participant, dtoEndTime);

        int taskIndex = 1;
        for(Task task : tasks) {
            taskList += taskIndex++ + ". " + task.getTaskName() + ", 만료기한: " + task.getEndTime() + "\n";
        }

        return taskList;
    }

    public String getChatbotHistory(ChatbotInfoReqDto reqDto) {
        // 워크스페이스 참여자 검증
        WorkspaceParticipant participant = workspaceParticipantRepository
                .findByWorkspaceIdAndUserId(reqDto.getWorkspaceId(), UUID.fromString(reqDto.getUserId()))
                .orElseThrow(() -> new EntityNotFoundException("워크스페이스 참여자가 아닙니다."));

        // 최근 메시지 20개만 가져오기
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<ChatbotMessage> recentMessages =
                chatbotMessageRepository.findByWorkspaceParticipant(participant, pageable);

        String history = "";
        history += "사용자 요청: " + reqDto.getContent() + "\n\n";
        history += "History: " + "\n";
        for (ChatbotMessage chatbotMessage : recentMessages) {
            history += chatbotMessage.getType() + ": " + chatbotMessage.getContent() + "\n";
        }
        return history;
    }
}
