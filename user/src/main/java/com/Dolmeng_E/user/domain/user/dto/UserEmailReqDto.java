package com.Dolmeng_E.user.domain.user.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEmailReqDto {
    @NotEmpty(message = "이메일이 비어있습니다.")
    @Size(max = 100)
    private String email;
}

