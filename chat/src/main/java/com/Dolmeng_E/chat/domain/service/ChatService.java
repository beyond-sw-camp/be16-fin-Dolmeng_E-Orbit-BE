package com.Dolmeng_E.chat.domain.service;

import com.Dolmeng_E.chat.common.dto.UserInfoResDto;
import com.Dolmeng_E.chat.domain.dto.ChatCreateReqDto;
import com.Dolmeng_E.chat.domain.dto.ChatMessageDto;
import com.Dolmeng_E.chat.domain.dto.ChatRoomListResDto;
import com.Dolmeng_E.chat.domain.entity.ChatMessage;
import com.Dolmeng_E.chat.domain.entity.ChatParticipant;
import com.Dolmeng_E.chat.domain.entity.ChatRoom;
import com.Dolmeng_E.chat.domain.entity.ReadStatus;
import com.Dolmeng_E.chat.domain.feignclient.UserFeignClient;
import com.Dolmeng_E.chat.domain.repository.ChatMessageRepository;
import com.Dolmeng_E.chat.domain.repository.ChatRoomRepository;
import com.Dolmeng_E.chat.domain.repository.ReadStatusRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ReadStatusRepository readStatusRepository;
    private final UserFeignClient userFeignClient;

    // 채팅 메시지 저장
    public void saveMessage(Long roomId, ChatMessageDto chatMessageDto) {
        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(() -> new EntityNotFoundException("없는 채팅방입니다."));

        // 보낸사람 조회
        UserInfoResDto senderInfo = userFeignClient.fetchUserInfo(chatMessageDto.getSenderEmail());

        // 메시지 저장
        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .userId(senderInfo.getUserId())
                .content(chatMessageDto.getMessage())
                .build();

        chatMessageRepository.save(chatMessage);

        // 사용자별로 읽음여부 저장
        List<ChatParticipant> chatParticipantList = chatRoom.getChatParticipantList();
        for(ChatParticipant chatParticipant : chatParticipantList) {
            ReadStatus readStatus = ReadStatus.builder()
                    .chatRoom(chatRoom)
                    .userId(senderInfo.getUserId())
                    .chatMessage(chatMessage)
                    .isRead(chatParticipant.getUserId().equals(senderInfo.getUserId()))
                    .build();
            readStatusRepository.save(readStatus);
        }
    }

    // 채팅방 생성
    public void createChatRoom(ChatCreateReqDto dto) {
        // 채팅방 생성
        ChatRoom chatRoom = ChatRoom.builder()
                .workspaceId(dto.getWorkspaceId())
                .projectId(dto.getProjectId())
                .stoneId(dto.getStoneId())
                .name(dto.getRoomName())
                .build();
        // 초대한 인원들을 참여자로 등록
        for(UUID userId : dto.getUserIdList()) {
            ChatParticipant chatParticipant = ChatParticipant.builder()
                    .chatRoom(chatRoom)
                    .userId(userId)
                    .build();
            chatRoom.getChatParticipantList().add(chatParticipant);
        }
        chatRoomRepository.save(chatRoom);
    }

    // 채팅방 목록 조회
    public List<ChatRoomListResDto> getChatRoomListByWorkspace(String workspaceId, String email) {
        UUID userId = userFeignClient.fetchUserInfo(email).getUserId();
        List<ChatRoom> chatRoomList = chatRoomRepository.findAllByUserAndWorkspace(userId, workspaceId);
        List<ChatRoomListResDto> chatRoomListResDtoList = chatRoomList.stream().map(c -> ChatRoomListResDto.fromEntity(c)).toList();

        return chatRoomListResDtoList;
    }

    // 해당 room의 참여자가 맞는 지 검증
    public boolean isRoomParticipant(String email, Long roomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(() -> new EntityNotFoundException("없는 채팅방입니다."));
        UUID userId = userFeignClient.fetchUserInfo(email).getUserId();

        return chatRoom.getChatParticipantList().stream()
                .anyMatch(p -> p.getUserId().equals(userId));
    }

    // 채팅방 채팅 목록 조회
    public List<ChatMessageDto> getChatListByRoom(Long roomId) {
        // roomId의 모든 채팅 가져와고
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(() -> new EntityNotFoundException("없는 채팅방입니다."));
        List<ChatMessage> chatMessages = chatMessageRepository.findByChatRoomOrderByCreatedAtAsc(chatRoom);

        // 각 채팅의 사용자 email을 dto에 담아 저장
        List<ChatMessageDto> chatMessageDtoList = new ArrayList<>();
        for(ChatMessage c : chatMessages) {
            String email = userFeignClient.fetchUserInfoById(String.valueOf(c.getUserId())).getUserEmail();
            chatMessageDtoList.add(ChatMessageDto.fromEntity(c, email));
        }

        return chatMessageDtoList;
    }


}
