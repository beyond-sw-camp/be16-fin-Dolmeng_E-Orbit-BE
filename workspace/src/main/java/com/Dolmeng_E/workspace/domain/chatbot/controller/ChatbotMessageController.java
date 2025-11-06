package com.Dolmeng_E.workspace.domain.chatbot.controller;

import com.Dolmeng_E.workspace.domain.chatbot.dto.*;
import com.Dolmeng_E.workspace.domain.chatbot.service.ChatbotMessageService;
import com.example.modulecommon.dto.CommonSuccessDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
public class ChatbotMessageController {
    private final ChatbotMessageService chatbotMessageService;

    // 사용자가 챗봇에게 메시지 전송
    @PostMapping("/message")
    public ResponseEntity<?> sendMessage(@RequestHeader("X-User-Id") String userId, @RequestBody @Valid ChatbotMessageUserReqDto chatbotMessageUserReqDto) {
        N8nResDto response = chatbotMessageService.sendMessage(userId, chatbotMessageUserReqDto);
        return new ResponseEntity<>(new CommonSuccessDto(response, HttpStatus.OK.value(), "챗봇에게 메시지 전송 성공"),  HttpStatus.OK);
    }

    // 프론트 -> 서버 -> agent -> 응답 반환
    @GetMapping("/message/chat-room/{roomId}")
    public ResponseEntity<?> sendRequestForChat(@RequestHeader("X-User-Id") String userId, @PathVariable("roomId") Long roomId) {
        N8nResDto response = chatbotMessageService.sendRequestForChat(userId, roomId);
        return new ResponseEntity<>(new CommonSuccessDto(response, HttpStatus.OK.value(), "Agent에게 메시지 전송 성공"),  HttpStatus.OK);
    }

    // 사용자와 챗봇의 대화 조회
    @GetMapping("/workspaces/{workspaceId}/chat/messages")
    public ResponseEntity<?> getUserMessageList(@RequestHeader("X-User-Id") String userId, @PathVariable String workspaceId) {
        List<ChatbotMessageListResDto> chatbotMessageListResDtoList = chatbotMessageService.getUserMessageList(userId, workspaceId);
        return new ResponseEntity<>(new CommonSuccessDto(chatbotMessageListResDtoList, HttpStatus.OK.value(), "챗봇과의 메시지 조회 성공"),  HttpStatus.OK);
    }

    // 프로젝트 대시보드 분석용
    @GetMapping("/project/{projectId}/dashboard")
    public ResponseEntity<?> analyzeProjectDashBoard(@RequestHeader("X-User-Id") String userId, @PathVariable String projectId) {
        AIProjectAnalysisResDto result = chatbotMessageService.analyzeProjectDashBoard(userId, projectId);
        return new ResponseEntity<>(new CommonSuccessDto(result, HttpStatus.OK.value(), "프로젝트 대시보드 분석정보 조회 성공"),  HttpStatus.OK);
    }

    // Agent전용
    // 프로젝트 요약을 위한 정보 제공
    @GetMapping("/project-info")
    public ResponseEntity<?> getProjectInfo(@ModelAttribute ChatbotInfoReqDto chatbotInfoReqDto) {
        String projectInfo = chatbotMessageService.getProjectInfo(chatbotInfoReqDto);
        return new ResponseEntity<>(new CommonSuccessDto(projectInfo, HttpStatus.OK.value(), "프로젝트 정보 조회 성공"),  HttpStatus.OK);
    }

    // 사용자가 할당 된 모든 task리스트 반환
    @GetMapping("/task-list")
    public ResponseEntity<?> getTaskList(@ModelAttribute ChatbotInfoReqDto chatbotInfoReqDto) {
        String taskList = chatbotMessageService.getTaskList(chatbotInfoReqDto);
        return new ResponseEntity<>(new CommonSuccessDto(taskList, HttpStatus.OK.value(), "Task 목록 조회 성공"),  HttpStatus.OK);
    }

    // 사용자와 챗봇의 최근 메시지 목록 조회
    @GetMapping("/history")
    public ResponseEntity<?> getChatbotHistory(@ModelAttribute ChatbotInfoReqDto chatbotInfoReqDto) {
        String chatbotHistory = chatbotMessageService.getChatbotHistory(chatbotInfoReqDto);
        return new ResponseEntity<>(new CommonSuccessDto(chatbotHistory, HttpStatus.OK.value(), "chatbot message 목록 조회 성공"),  HttpStatus.OK);
    }
}
