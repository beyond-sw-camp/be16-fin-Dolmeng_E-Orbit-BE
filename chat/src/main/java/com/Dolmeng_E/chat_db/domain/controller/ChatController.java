package com.Dolmeng_E.chat_db.domain.controller;

import com.Dolmeng_E.chat_db.domain.dto.*;
import com.Dolmeng_E.chat_db.domain.entity.ChatRoom;
import com.Dolmeng_E.chat_db.domain.service.ChatService;
import com.example.modulecommon.dto.CommonSuccessDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    // 채팅방에 인원 초대
    @PostMapping("/room/new-user")
    public ResponseEntity<?> inviteChatParticipants(@RequestBody @Valid ChatInviteReqDto dto) {
        ChatRoom chatRoom = chatService.inviteChatParticipants(dto);
        return new ResponseEntity<>(new CommonSuccessDto(chatRoom.getName(), HttpStatus.OK.value(), "채팅방 초대 성공"), HttpStatus.OK);
    }

    // 채팅방 목록 조회
    @GetMapping("/room/list/{workspaceId}")
    public ResponseEntity<?> getChatRoomListByWorkspace(@PathVariable String workspaceId, @RequestHeader("X-User-Id")String userId) {
        List<ChatRoomListResDto> chatRoomList = chatService.getChatRoomListByWorkspace(workspaceId, userId);
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
    public ResponseEntity<?> messageRead(@PathVariable("roomId") Long roomId, @RequestHeader("X-User-Id")String userId) {
        chatService.messageRead(roomId, userId);
        return new ResponseEntity<>(new CommonSuccessDto(userId, HttpStatus.OK.value(), "채팅방 전체채팅 읽음 성공"), HttpStatus.OK);
    }

    // 파일 업로드
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFileList(@RequestParam("fileList") List<MultipartFile> fileList) {
        List<ChatFileListDto> chatFileListResDtoIdList = chatService.uploadFileList(fileList);
        return new ResponseEntity<>(new CommonSuccessDto(chatFileListResDtoIdList, HttpStatus.OK.value(), "파일 업로드 성공"), HttpStatus.OK);
    }

    // 채팅방 참여자 목록 조회
    @GetMapping("/room/{roomId}/participants")
    public ResponseEntity<?> getParticipantListByRoom (@PathVariable("roomId") Long roomId) {
        List<ChatParticipantListResDto> participantListDto = chatService.getParticipantListByRoom(roomId);
        return new ResponseEntity<>(new CommonSuccessDto(participantListDto, HttpStatus.OK.value(), "채팅방 참여자 목록 조회 성공"), HttpStatus.OK);
    }

    // 채팅방 파일 목록 조회
    @GetMapping("/room/{roomId}/files")
    public ResponseEntity<?> getFileListByRoom (@PathVariable("roomId") Long roomId) {
        List<ChatFileListDto> fileListDto = chatService.getFileListByRoom(roomId);
        return new ResponseEntity<>(new CommonSuccessDto(fileListDto, HttpStatus.OK.value(), "채팅방 파일 목록 조회 성공"), HttpStatus.OK);
    }

    // 채팅방의 안읽은 메시지 목록 조회 (agent사용)
    @GetMapping("/room/{roomId}/unread-messages")
    public ResponseEntity<CommonSuccessDto> getUnreadMessageListByRoom (@PathVariable("roomId") Long roomId, @RequestHeader("X-User-Id")String userId) {
        String unreadMessages = chatService.getUnreadMessagesByRoom(roomId, userId);
        return new ResponseEntity<>(new CommonSuccessDto(unreadMessages, HttpStatus.OK.value(), "채팅방 안 읽은 메시지 목록 조회 성공"), HttpStatus.OK);
    }

    // 워크스페이스의 안 읽은 메시지 수 조회
    @GetMapping("/{workspaceId}/message-count")
    public ResponseEntity<?>  getMessageCountByWorkspace(@PathVariable("workspaceId") String workspaceId, @RequestHeader("X-User-Id")String userId) {
        Long unreadMessagesCount = chatService.getMessageCountByWorkspace(workspaceId, userId);
        return new ResponseEntity<>(new CommonSuccessDto(unreadMessagesCount, HttpStatus.OK.value(), "워크스페이스 안 읽은 메시지 수 조회 성공"), HttpStatus.OK);
    }

}
