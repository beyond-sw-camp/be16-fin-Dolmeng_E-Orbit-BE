package com.Dolmeng_E.user.domain.user.dto;

import com.Dolmeng_E.user.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDetailResDto {
    private String name;
    private String email;
    private String phoneNumber;
    private String profileImageUrl;
    private boolean isDelete;

    public static UserDetailResDto fromEntity(User user) {
        return UserDetailResDto.builder()
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .profileImageUrl(user.getProfileImageUrl())
                .isDelete(user.isDelete())
                .build();
    }
}
