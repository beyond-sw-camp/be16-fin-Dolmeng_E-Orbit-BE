package com.Dolmeng_E.chat.domain.dto;

import com.Dolmeng_E.chat.common.dto.UserInfoResDto;
import com.Dolmeng_E.chat.domain.entity.ChatParticipant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatParticipantListResDto {
    private String name;
    private String profileImageUrl;

    public static ChatParticipantListResDto from(UserInfoResDto userInfoResDto) {
        return ChatParticipantListResDto.builder()
                .name(userInfoResDto.getUserName())
                .profileImageUrl(userInfoResDto.getProfileImageUrl())
                .build();
    }
}
