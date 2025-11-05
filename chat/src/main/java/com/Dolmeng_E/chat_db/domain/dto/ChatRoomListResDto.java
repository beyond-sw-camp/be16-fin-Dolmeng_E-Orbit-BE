package com.Dolmeng_E.chat_db.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomListResDto {
    private Long roomId;
    private String roomName;
    private int participantCount; // 참여자 수
    private String lastMessage;    // 마지막 메시지 내용
    private LocalDateTime lastSendTime; // 마지막 메시지 시간
    private Long unreadCount;       // 현재 사용자의 안읽은 메시지 수
    private Boolean isVideoCallActive;
    @Builder.Default
    private List<String> userProfileImageUrlList = new ArrayList<>(); // 사용자의 프로필 이미지 리스트
}
