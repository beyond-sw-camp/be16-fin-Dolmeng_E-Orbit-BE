package com.Dolmeng_E.chat.domain.controller;

import com.Dolmeng_E.chat.domain.dto.ChatCreateReqDto;
import com.Dolmeng_E.chat.domain.dto.ChatRoomListResDto;
import com.Dolmeng_E.chat.domain.dto.ChatRoomListReqDto;
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
    @GetMapping("/room/list")
    public ResponseEntity<?> getChatRoomListByWorkspace(@ModelAttribute ChatRoomListReqDto dto) {
        List<ChatRoomListResDto> chatRoomList = chatService.getChatRoomListByWorkspace(dto);
        return new ResponseEntity<>(new CommonSuccessDto(chatRoomList, HttpStatus.OK.value(), "채팅방 목록 조회 성공"), HttpStatus.OK);
    }

}
