package com.Dolmeng_E.chat_db.domain.service;

import com.Dolmeng_E.chat_db.common.dto.NotificationCreateReqDto;
import com.Dolmeng_E.chat_db.common.dto.UserInfoResDto;
import com.Dolmeng_E.chat_db.common.service.S3Uploader;
import com.Dolmeng_E.chat_db.domain.dto.*;
import com.Dolmeng_E.chat_db.domain.entity.*;
import com.Dolmeng_E.chat_db.domain.feignclient.UserFeignClient;
import com.Dolmeng_E.chat_db.domain.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private final S3Uploader s3Uploader;
    private final ChatFileRepository chatFileRepository;
    private final ChatParticipantRepository chatParticipantRepository;

    public ChatService(ChatRoomRepository chatRoomRepository, ChatMessageRepository chatMessageRepository, ReadStatusRepository readStatusRepository, UserFeignClient userFeignClient, @Qualifier("rtInventory") RedisTemplate<String, String> redisTemplate, SimpMessageSendingOperations messageTemplate, S3Uploader s3Uploader, ChatFileRepository chatFileRepository, ChatParticipantRepository chatParticipantRepository) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.readStatusRepository = readStatusRepository;
        this.userFeignClient = userFeignClient;
        this.redisTemplate = redisTemplate;
        this.messageTemplate = messageTemplate;
        this.s3Uploader = s3Uploader;
        this.chatFileRepository = chatFileRepository;
        this.chatParticipantRepository = chatParticipantRepository;
    }

    // 채팅 메시지 저장
    public ChatMessageDto saveMessage(Long roomId, ChatMessageDto chatMessageDto) {
        // 반환을 위한 dto
        ChatMessageDto newChatMessageDto = chatMessageDto;
        newChatMessageDto.setRoomId(roomId);

        // 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(() -> new EntityNotFoundException("없는 채팅방입니다."));

        // 보낸사람 조회
        UserInfoResDto senderInfo = userFeignClient.fetchUserInfoById(chatMessageDto.getSenderId());

        // 메시지 저장
        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .userId(senderInfo.getUserId())
                .userName(senderInfo.getUserName())
                .content(chatMessageDto.getMessage())
                .type(chatMessageDto.getMessageType())
                .build();
        chatMessageRepository.save(chatMessage);

        // 사용자별로 읽음여부 저장 (현재 방에 접속 중인 사용자와 비교)
        Set<String> connectedUsers = redisTemplate.opsForSet().members("chat:room:" + roomId + ":users");

        List<ChatParticipant> chatParticipantList = chatRoom.getChatParticipantList();
        for(ChatParticipant chatParticipant : chatParticipantList) {
            ReadStatus readStatus = ReadStatus.builder()
                    .chatRoom(chatRoom)
                    .userId(chatParticipant.getUserId())
                    .chatMessage(chatMessage)
                    .isRead(connectedUsers.contains(String.valueOf(chatParticipant.getUserId())))
                    .build();
            readStatusRepository.save(readStatus);
        }

        // 파일 있으면 파일 저장
        if(chatMessageDto.getMessageType() != MessageType.TEXT) {
            List<ChatFileListDto> fileListCopy = new ArrayList<>(chatMessageDto.getChatFileListDtoList());
            for (ChatFileListDto chatFileListDto : fileListCopy) {
                ChatFile chatFile = getChatFile(chatFileListDto.getFileId());
                chatFile.setChatMessage(chatMessage);

                chatFileListDto.setFileName(chatFile.getFileName());
                chatFileListDto.setFileSize(chatFile.getFileSize());
                chatFileListDto.setFileUrl(chatFile.getFileUrl());
            }
        }
        return newChatMessageDto;
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

    // 채팅방에 인원 초대
    public ChatRoom inviteChatParticipants(ChatInviteReqDto dto) {
        // 채팅방 조회
        ChatRoom chatRoom =
                chatRoomRepository.findByWorkspaceIdAndProjectIdAndStoneIdAndIsDelete(dto.getWorkspaceId(),
                        dto.getProjectId(),
                        dto.getStoneId(),
                        "N").orElseThrow(() -> new EntityNotFoundException("없는 채팅방입니다."));

        // 초대한 인원들을 참여자로 등록
        for(UUID userId : dto.getUserIdList()) {
            ChatParticipant chatParticipant = ChatParticipant.builder()
                    .chatRoom(chatRoom)
                    .userId(userId)
                    .build();
            chatRoom.getChatParticipantList().add(chatParticipant);
        }

        return chatRoom;
    }

    // 채팅방 목록 조회
    public List<ChatRoomListResDto> getChatRoomListByWorkspace(String workspaceId, String userId) {
        UserInfoResDto senderInfo = userFeignClient.fetchUserInfoById(userId);
        // 워크스페이스에서 사용자가 포함된 채팅방 조회
        List<ChatRoom> chatRoomList = chatRoomRepository.findAllByUserAndWorkspace(senderInfo.getUserId(), workspaceId);

        List<ChatRoomListResDto> chatRoomListResDtoList = new ArrayList<>();

        for (ChatRoom room : chatRoomList) {
            ChatMessage chatMessage = null;
            if(!room.getChatMessageList().isEmpty()) {
                chatMessage = room.getChatMessageList().get(room.getChatMessageList().size() - 1);
            }

            Long unreadCount = readStatusRepository
                    .countByUserIdAndChatRoom_IdAndIsReadFalse(senderInfo.getUserId(), room.getId());

            List<String> userProfileImageUrlList = new ArrayList<>();
            for (ChatParticipant p : room.getChatParticipantList()) {
                if(!p.getUserId().equals(senderInfo.getUserId())) {
                    String userProfileImageUrl = userFeignClient.fetchUserInfoById(String.valueOf(p.getUserId())).getProfileImageUrl();
                    userProfileImageUrlList.add(userProfileImageUrl);
                }
            }

            ChatRoomListResDto chatRoomDto = ChatRoomListResDto.builder()
                    .roomId(room.getId())
                    .roomName(room.getName())
                    .participantCount(room.getChatParticipantList().size())
                    .lastMessage(chatMessage != null ? chatMessage.getContent() : "메시지가 없습니다.")
                    .lastSendTime(chatMessage != null ? chatMessage.getCreatedAt() : null)
                    .unreadCount(unreadCount)
                    .userProfileImageUrlList(userProfileImageUrlList)
                    .build();

            chatRoomListResDtoList.add(chatRoomDto);
        }
        return chatRoomListResDtoList;
    }

    // 해당 room의 참여자가 맞는 지 검증
    public boolean isRoomParticipant(String userId, Long roomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(() -> new EntityNotFoundException("없는 채팅방입니다."));

        return chatRoom.getChatParticipantList().stream()
                .anyMatch(p -> p.getUserId().equals(UUID.fromString(userId)));
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
                    .senderId(String.valueOf(senderInfo.getUserId()))
                    .senderName(senderInfo.getUserName())
                    .message(c.getContent())
                    .lastSendTime(c.getCreatedAt())
                    .userProfileImageUrl(senderInfo.getProfileImageUrl())
                    .messageType(c.getType())
                    .chatFileListDtoList(c.getChatFileList().stream().map(chatFile -> ChatFileListDto.fromEntity(chatFile)).toList())
                    .build();

            chatMessageDtoList.add(chatMessageDto);
        }

        return chatMessageDtoList;
    }

    // 특정 room의 모든 메시지 읽음 처리
    public void messageRead(Long roomId, String userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(() -> new EntityNotFoundException("없는 채팅방입니다."));
        UserInfoResDto userInfo = userFeignClient.fetchUserInfoById(userId);

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
                    .lastSenderId(dto.getSenderId())
                    .unreadCount(unreadCount)
                    .messageType(dto.getMessageType())
                    .build();

            // 각 사용자별 summary 토픽 전송
            messageTemplate.convertAndSend("/topic/summary/" + p.getUserId(), summary);
        }
    }

    public List<NotificationCreateReqDto> createNotification(ChatMessageDto dto) {
        ChatRoom room = chatRoomRepository.findById(dto.getRoomId())
                .orElseThrow(() -> new EntityNotFoundException("없는 채팅방입니다."));

        List<NotificationCreateReqDto> notificationCreateReqDtoList = new ArrayList<>();
        for (ChatParticipant p : room.getChatParticipantList()) {
            Long unreadCount = readStatusRepository
                    .countUnreadMessagesInWorkspaceByUser(room.getWorkspaceId(), p.getUserId());

            List<UUID> userIdList = new ArrayList<>();
            userIdList.add(p.getUserId());

            NotificationCreateReqDto notificationCreateReqDto = NotificationCreateReqDto.builder()
                    .title(Long.toString(unreadCount))
                    .type("NEW_CHAT_MESSAGE")
                    .userIdList(userIdList)
                    .build();

            notificationCreateReqDtoList.add(notificationCreateReqDto);
        }

        return notificationCreateReqDtoList;
    }

    // 파일 업로드
    public List<ChatFileListDto> uploadFileList(List<MultipartFile> fileList) {
        List<ChatFileListDto> chatFileListDtoList = new ArrayList<>();

        for(MultipartFile file : fileList) {
            String fileUrl = s3Uploader.upload(file, "chat");

            ChatFile chatFile = ChatFile.builder()
                    .fileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .fileUrl(fileUrl)
                    .build();

            chatFileRepository.save(chatFile);

            chatFileListDtoList.add(ChatFileListDto.builder().fileId(chatFile.getId()).build());
        }

        return chatFileListDtoList;
    }

    public ChatFile getChatFile(Long id) {
        return chatFileRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("없는 파일입니다."));
    }

    // 채팅방 참여자 목록 조회
    public List<ChatParticipantListResDto> getParticipantListByRoom(Long roomId) {
        List<ChatParticipant> chatParticipantList = chatParticipantRepository.findByChatRoomId(roomId);
        List<ChatParticipantListResDto> chatParticipantListResDtoList = new ArrayList<>();

        for(ChatParticipant p : chatParticipantList) {
            UserInfoResDto senderInfo = userFeignClient.fetchUserInfoById(String.valueOf(p.getUserId()));
            chatParticipantListResDtoList.add(ChatParticipantListResDto.from(senderInfo));
        }

        return chatParticipantListResDtoList;
    }

    // 채팅방 파일 목록 조회
    public List<ChatFileListDto> getFileListByRoom(Long roomId) {
        List<ChatFile> chatFileList = chatFileRepository.findAllByRoomId(roomId);
        List<ChatFileListDto> chatFileListDtoList = chatFileList.stream().map(chatFile -> ChatFileListDto.fromEntity(chatFile)).toList();
        return chatFileListDtoList;
    }

    // Agent 겸용 기능
    public String getUnreadMessagesByRoom(Long roomId, String userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("없는 채팅방입니다."));

        // room이랑 user로 안 읽은 메시지 목록 먼저 가져오고, 그거에 매핑되는 메시지
        List<ChatMessage> chatMessageList = readStatusRepository.findUnreadMessagesByChatRoomAndUserId(chatRoom, UUID.fromString(userId));

        String unreadMessageList = "";
        for(ChatMessage chatMessage : chatMessageList) {
            unreadMessageList += chatMessage.getUserName() + " : " + chatMessage.getContent() + "\n";
        }

        return unreadMessageList;
    }

    // 워크스페이스 내에서 사용자가 읽지 않은 메시지 전부 조회
    public String getUnreadMessages(ChatbotUnreadMessageListReqDto dto) {
        // 사용자, 워크스페이스 필터링
        List<ChatRoom> chatRoomList = chatRoomRepository.findAllByUserAndWorkspace(UUID.fromString(dto.getUserId()), dto.getWorkspaceId());

        String unreadMessageList = "";

        if(!chatRoomList.isEmpty()) {
            for(ChatRoom chatRoom : chatRoomList) {
                unreadMessageList += "채팅방 이름: " + chatRoom.getName() + "\n";

                List<ChatMessage> chatMessageList = readStatusRepository.findUnreadMessagesByChatRoomAndUserId(chatRoom, UUID.fromString(dto.getUserId()));

                if(!chatMessageList.isEmpty()) {
                    for(ChatMessage chatMessage : chatMessageList) {
                        unreadMessageList += chatMessage.getUserName() + " : " + chatMessage.getContent() + "\n";
                    }
                } else {
                    unreadMessageList += "읽지 않은 채팅이 없습니다.\n";
                }
            }
        } else {
            unreadMessageList += "채팅방이 없습니다.";
        }

        return unreadMessageList;
    }

}
