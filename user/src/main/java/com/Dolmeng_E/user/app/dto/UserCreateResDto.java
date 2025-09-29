package com.Dolmeng_E.user.app.dto;

import com.Dolmeng_E.user.app.domain.User;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class UserCreateResDto {
    @NotEmpty(message = "이름이 비어있습니다.")
    @Size(max = 50)
    private String name;

    @NotEmpty(message = "이메일이 비어있습니다.")
    @Size(max = 100)
    private String email;

    @NotEmpty(message = "이메일이 비어있습니다.")
    @Size(min = 8, message = "비밀번호 길이가 8자 이내입니다.")
    private String password;

    private String phoneNumber;

    private MultipartFile profileImageUrl;

    public User toEntity(String encodedPassword, String profileImageUrl) {
        return User.builder()
                .name(this.name)
                .email(this.email)
                .password(encodedPassword)
                .phoneNumber(this.phoneNumber)
                .profileImageUrl(profileImageUrl)
                .build();
    }
}
