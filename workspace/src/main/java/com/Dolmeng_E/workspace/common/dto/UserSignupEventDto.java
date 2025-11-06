package com.Dolmeng_E.workspace.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSignupEventDto {
    private String userId;
    private String userName;
    private String email;
    private LocalDateTime createdAt;
}