package com.Dolmeng_E.chat.domain.controller;

import com.Dolmeng_E.chat.domain.dto.ChatCreateReqDto;
import com.Dolmeng_E.chat.domain.dto.ChatMessageDto;
import com.Dolmeng_E.chat.domain.dto.ChatRoomListResDto;
import com.Dolmeng_E.chat.domain.service.ChatService;
import com.example.modulecommon.dto.CommonSuccessDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {
    private final ChatService chatService;

    // 채팅방 생성
    @PostMapping("/room")
    public ResponseEntity<?> createChatRoom(@RequestBody @Valid ChatCreateReqDto dto) {
        chatService.createChatRoom(dto);
        return new ResponseEntity<>(new CommonSuccessDto(dto.getRoomName(), HttpStatus.OK.value(), "채팅방 생성 성공"), HttpStatus.OK);
    }

    // 채팅방 목록 조회
    @GetMapping("/room/list/{workspaceId}")
    public ResponseEntity<?> getChatRoomListByWorkspace(@PathVariable String workspaceId, @RequestHeader("X-User-Email")String email) {
        List<ChatRoomListResDto> chatRoomList = chatService.getChatRoomListByWorkspace(workspaceId, email);
        return new ResponseEntity<>(new CommonSuccessDto(chatRoomList, HttpStatus.OK.value(), "채팅방 목록 조회 성공"), HttpStatus.OK);
    }

    // 채팅방 채팅 목록 조회
    @GetMapping("/room/{roomId}/history")
    public ResponseEntity<?> getChatListByRoom(@PathVariable("roomId") Long roomId) {
        List<ChatMessageDto> chatMessageDtoList = chatService.getChatListByRoom(roomId);
        return new ResponseEntity<>(new CommonSuccessDto(chatMessageDtoList, HttpStatus.OK.value(), "채팅방 채팅 목록 조회 성공"), HttpStatus.OK);
    }

    // 특정 room의 모든 메시지 읽음 처리
    @PostMapping("/room/{roomId}/read_status")
    public ResponseEntity<?> messageRead(@PathVariable("roomId") Long roomId, @RequestHeader("X-User-Email")String email) {
        chatService.messageRead(roomId, email);
        return new ResponseEntity<>(new CommonSuccessDto(email, HttpStatus.OK.value(), "채팅방 전체채팅 읽음 성공"), HttpStatus.OK);
    }

}
