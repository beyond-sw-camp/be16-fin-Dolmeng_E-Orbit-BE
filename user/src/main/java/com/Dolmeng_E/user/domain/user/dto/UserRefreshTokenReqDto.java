package com.Dolmeng_E.user.domain.user.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class UserRefreshTokenReqDto {
    @NotEmpty(message = "Refresh Token이 비어있습니다.")
    private String refreshToken;

    private boolean rememberMe = false;
}
