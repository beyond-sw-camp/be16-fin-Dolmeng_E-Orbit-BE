package com.Dolmeng_E.chat.domain.service;

import com.Dolmeng_E.chat.common.dto.UserInfoResDto;
import com.Dolmeng_E.chat.domain.dto.ChatCreateReqDto;
import com.Dolmeng_E.chat.domain.dto.ChatMessageDto;
import com.Dolmeng_E.chat.domain.dto.ChatRoomListResDto;
import com.Dolmeng_E.chat.domain.dto.ChatSummaryDto;
import com.Dolmeng_E.chat.domain.entity.ChatMessage;
import com.Dolmeng_E.chat.domain.entity.ChatParticipant;
import com.Dolmeng_E.chat.domain.entity.ChatRoom;
import com.Dolmeng_E.chat.domain.entity.ReadStatus;
import com.Dolmeng_E.chat.domain.feignclient.UserFeignClient;
import com.Dolmeng_E.chat.domain.repository.ChatMessageRepository;
import com.Dolmeng_E.chat.domain.repository.ChatRoomRepository;
import com.Dolmeng_E.chat.domain.repository.ReadStatusRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class ChatService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ReadStatusRepository readStatusRepository;
    private final UserFeignClient userFeignClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final SimpMessageSendingOperations messageTemplate;

    public ChatService(ChatRoomRepository chatRoomRepository, ChatMessageRepository chatMessageRepository, ReadStatusRepository readStatusRepository, UserFeignClient userFeignClient, @Qualifier("rtInventory") RedisTemplate<String, String> redisTemplate, SimpMessageSendingOperations messageTemplate) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.readStatusRepository = readStatusRepository;
        this.userFeignClient = userFeignClient;
        this.redisTemplate = redisTemplate;
        this.messageTemplate = messageTemplate;
    }

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

        // 사용자별로 읽음여부 저장 (현재 방에 접속 중인 사용자와 비교)
        Set<String> connectedUsers = redisTemplate.opsForSet().members("chat:room:" + roomId + ":users");

        List<ChatParticipant> chatParticipantList = chatRoom.getChatParticipantList();
        for(ChatParticipant chatParticipant : chatParticipantList) {
            String email = userFeignClient.fetchUserInfoById(String.valueOf(chatParticipant.getUserId())).getUserEmail();
            ReadStatus readStatus = ReadStatus.builder()
                    .chatRoom(chatRoom)
                    .userId(chatParticipant.getUserId())
                    .chatMessage(chatMessage)
                    .isRead(connectedUsers.contains(email))
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
        UserInfoResDto senderInfo = userFeignClient.fetchUserInfo(email);
        // 워크스페이스에서 사용자가 포함된 채팅방 조회
        List<ChatRoom> chatRoomList = chatRoomRepository.findAllByUserAndWorkspace(senderInfo.getUserId(), workspaceId);

        List<ChatRoomListResDto> chatRoomListResDtoList = new ArrayList<>();

        for (ChatRoom room : chatRoomList) {
            ChatMessage chatMessage = room.getChatMessageList().get(room.getChatMessageList().size() - 1);
            Long unreadCount = readStatusRepository
                    .countByUserIdAndChatRoom_IdAndIsReadFalse(senderInfo.getUserId(), room.getId());

            List<String> userProfileImageUrlList = new ArrayList<>();
            for (ChatParticipant p : room.getChatParticipantList()) {
                String userProfileImageUrl = userFeignClient.fetchUserInfoById(String.valueOf(p.getUserId())).getProfileImageUrl();
                userProfileImageUrlList.add(userProfileImageUrl);
            }

            ChatRoomListResDto chatRoomDto = ChatRoomListResDto.builder()
                    .roomId(room.getId())
                    .roomName(room.getName())
                    .participantCount(room.getChatParticipantList().size())
                    .lastMessage(chatMessage.getContent())
                    .lastSendTime(chatMessage.getCreatedAt())
                    .unreadCount(unreadCount)
                    .userProfileImageUrlList(userProfileImageUrlList)
                    .build();

            chatRoomListResDtoList.add(chatRoomDto);
        }
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
            UserInfoResDto senderInfo = userFeignClient.fetchUserInfoById(String.valueOf(c.getUserId()));

            ChatMessageDto chatMessageDto = ChatMessageDto.builder()
                    .senderEmail(senderInfo.getUserEmail())
                    .senderName(senderInfo.getUserName())
                    .message(c.getContent())
                    .lastSendTime(c.getCreatedAt())
                    .userProfileImageUrl(senderInfo.getProfileImageUrl())
                    .build();

            chatMessageDtoList.add(chatMessageDto);
        }

        return chatMessageDtoList;
    }

    // 특정 room의 모든 메시지 읽음 처리
    public void messageRead(Long roomId, String email) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(() -> new EntityNotFoundException("없는 채팅방입니다."));
        UserInfoResDto userInfo = userFeignClient.fetchUserInfo(email);

        // 채팅방에 접속한 사용자의 모든 읽음 여부 가져와 true 설정 (접속한 채팅방의 읽음 여부)
        List<ReadStatus> readStatuses = chatRoom.getReadStatusList().stream().filter(r -> r.getUserId().equals(userInfo.getUserId())).toList();
        for(ReadStatus r : readStatuses){
            r.updateIsRead(true);
        }
    }

    // 채팅 목록 업데이트를 위한 브로드캐스트
    public void broadcastChatSummary(ChatMessageDto dto) {
        ChatRoom room = chatRoomRepository.findById(dto.getRoomId())
                .orElseThrow(() -> new EntityNotFoundException("없는 채팅방입니다."));

        for (ChatParticipant p : room.getChatParticipantList()) {
            Long unreadCount = readStatusRepository
                    .countByUserIdAndChatRoom_IdAndIsReadFalse(p.getUserId(), room.getId());

            ChatSummaryDto summary = ChatSummaryDto.builder()
                    .roomId(room.getId())
                    .lastMessage(dto.getMessage())
                    .lastSendTime(dto.getLastSendTime())
                    .lastSenderEmail(dto.getSenderEmail())
                    .unreadCount(unreadCount)
                    .build();

            String email = userFeignClient.fetchUserInfoById(String.valueOf(p.getUserId())).getUserEmail();
            // 각 사용자별 summary 토픽 전송
            messageTemplate.convertAndSend("/topic/summary/" + email, summary);
        }
    }

    // 파일 전송
    


}
