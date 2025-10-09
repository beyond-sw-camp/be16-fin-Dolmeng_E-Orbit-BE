package com.Dolmeng_E.chat.domain.dto;

import com.Dolmeng_E.chat.domain.entity.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomListResDto {
    private Long roomId;
    private String roomName;

    public static ChatRoomListResDto fromEntity(ChatRoom chatRoom) {
        return ChatRoomListResDto.builder()
                .roomId(chatRoom.getId())
                .roomName(chatRoom.getName())
                .build();
    }
}
