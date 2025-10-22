package com.Dolmeng_E.user.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSignupEventDto {
    private String userId;
    private String userName;
    private String email;
    private LocalDateTime createdAt;
}