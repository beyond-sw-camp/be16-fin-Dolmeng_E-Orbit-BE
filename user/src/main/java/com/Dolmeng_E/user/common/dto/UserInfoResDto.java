package com.Dolmeng_E.user.common.dto;

import com.Dolmeng_E.user.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class UserInfoResDto {
    private UUID userId;
    private String userName;
    private String userEmail;
    private String profileImageUrl;

    public static UserInfoResDto fromEntity(User user) {
        return UserInfoResDto.builder()
                .userId(user.getId())
                .userName(user.getName())
                .userEmail(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }
}
