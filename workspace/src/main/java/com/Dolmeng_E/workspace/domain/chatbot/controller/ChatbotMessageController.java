package com.Dolmeng_E.workspace.domain.chatbot.controller;

import com.Dolmeng_E.workspace.domain.chatbot.dto.ChatbotMessageListResDto;
import com.Dolmeng_E.workspace.domain.chatbot.dto.ChatbotMessageUserReqDto;
import com.Dolmeng_E.workspace.domain.chatbot.dto.ChatbotTaskListReqDto;
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
        String response = chatbotMessageService.sendMessage(userId, chatbotMessageUserReqDto);
        return new ResponseEntity<>(new CommonSuccessDto(response, HttpStatus.OK.value(), "챗봇에게 메시지 전송 성공"),  HttpStatus.OK);
    }

    // 사용자와 챗봇의 대화 조회
    @GetMapping("/workspaces/{workspaceId}/chat/messages")
    public ResponseEntity<?> getUserMessageList(@RequestHeader("X-User-Id") String userId, @PathVariable String workspaceId) {
        List<ChatbotMessageListResDto> chatbotMessageListResDtoList = chatbotMessageService.getUserMessageList(userId, workspaceId);
        return new ResponseEntity<>(new CommonSuccessDto(chatbotMessageListResDtoList, HttpStatus.OK.value(), "챗봇과의 메시지 조회 성공"),  HttpStatus.OK);
    }

    // Agent전용
    // 프로젝트 요약을 위한 정보 제공
    @GetMapping("/project-info/{projectName}")
    public ResponseEntity<?> getProjectInfo(@PathVariable String projectName) {
        String projectInfo = chatbotMessageService.getProjectInfo(projectName);
        return new ResponseEntity<>(new CommonSuccessDto(projectInfo, HttpStatus.OK.value(), "프로젝트 정보 조회 성공"),  HttpStatus.OK);
    }

    // 사용자가 할당 된 모든 task리스트 반환 (프로젝트 별 - 스톤 - 태스크 별)
    @GetMapping("/task-list")
    public ResponseEntity<?> getTaskList(@ModelAttribute ChatbotTaskListReqDto  chatbotTaskListReqDto) {
        String taskList = chatbotMessageService.getTaskList(chatbotTaskListReqDto);
        return new ResponseEntity<>(new CommonSuccessDto(taskList, HttpStatus.OK.value(), "Task 목록 조회 성공"),  HttpStatus.OK);
    }
}
