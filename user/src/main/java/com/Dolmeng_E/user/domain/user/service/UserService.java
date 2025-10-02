package com.Dolmeng_E.user.domain.user.service;

import com.Dolmeng_E.user.domain.user.dto.UserInfoResDto;
import com.Dolmeng_E.user.domain.user.entity.User;
import com.Dolmeng_E.user.domain.user.dto.UserCreateReqDto;
import com.Dolmeng_E.user.domain.user.dto.UserLoginReqDto;
import com.Dolmeng_E.user.domain.user.repository.UserRepository;
import com.Dolmeng_E.user.common.service.S3Uploader;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3Uploader s3Uploader;

    public void create(UserCreateReqDto dto) {
        if(userRepository.findByEmail(dto.getEmail()).isPresent()) throw new EntityExistsException("중복되는 이메일입니다.");

        String encodedPassword = passwordEncoder.encode(dto.getPassword());
        String profileImgaeUrl = s3Uploader.upload(dto.getProfileImageUrl(), "user");

        User user = dto.toEntity(encodedPassword, profileImgaeUrl);
        userRepository.save(user);
    }

    public User login(UserLoginReqDto dto) {
        User user = userRepository.findByEmail(dto.getEmail()).orElseThrow(() -> new EntityNotFoundException("없는 회원입니다."));
        if(!passwordEncoder.matches(dto.getPassword(), user.getPassword())) throw new IllegalArgumentException("비밀번호가 틀렸습니다.");
        return user;
    }

    // 유저 ID, 이름 반환 API
    public UserInfoResDto fetchUserInfo(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow(()->new EntityNotFoundException("없는 회원입니다."));
        return UserInfoResDto.builder()
                .userId(user.getId())
                .userName(user.getEmail())
                .build();

    }

}
