package com.Dolmeng_E.chat.common.dto;

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
}
