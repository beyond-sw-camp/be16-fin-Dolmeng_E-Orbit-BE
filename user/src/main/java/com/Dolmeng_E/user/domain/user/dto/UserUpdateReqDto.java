package com.Dolmeng_E.user.domain.user.dto;

import com.Dolmeng_E.user.domain.user.entity.User;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class UserUpdateReqDto {
    private String name;
    private String phoneNumber;
    private MultipartFile profileImage;
}
