package com.Dolmeng_E.user.domain.user.service;

import com.Dolmeng_E.user.common.auth.JwtTokenProvider;
import com.Dolmeng_E.user.domain.user.dto.*;
import com.Dolmeng_E.user.domain.user.entity.User;
import com.Dolmeng_E.user.domain.user.repository.UserRepository;
import com.example.modulecommon.service.S3Uploader;
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
    private final JwtTokenProvider jwtTokenProvider;
    private final KakaoService kakaoService;

    // 회원가입 API
    public void create(UserCreateReqDto dto) {
        if(userRepository.findByEmail(dto.getEmail()).isPresent()) throw new EntityExistsException("중복되는 이메일입니다.");

        String encodedPassword = passwordEncoder.encode(dto.getPassword());
        String profileImgaeUrl = s3Uploader.upload(dto.getProfileImageUrl(), "user");

        User user = dto.toEntity(encodedPassword, profileImgaeUrl);
        userRepository.save(user);
    }

    // 로그인 API
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

    // 카카오 로그인 API (정보 없으면 회원가입까지)
    public UserLoginResDto kakaoLogin(RedirectDto dto) {
        // accessToken 발급
        AccessTokenDto accessTokenDto = kakaoService.getAccessToken(dto.getCode());
        // 사용자 정보 얻기
        KakaoProfileDto kakaoProfileDto = kakaoService.getKakaoProfile(accessTokenDto.getAccess_token());

        // 회원가입이 되어있지 않다면 회원가입
        User user = userRepository.findBySocialId(kakaoProfileDto.getId()).orElse(null);
        if(user == null) {
            user = kakaoService.createOauth(kakaoProfileDto);
            userRepository.save(user);
        }
        if(user.isDelete()) throw new IllegalArgumentException("탈퇴한 회원입니다.");

        // 토큰 생성해서 반환
        String accessToken = jwtTokenProvider.createAtToken(user);
        String refreshToken = jwtTokenProvider.createRtToken(user);

        // todo - 자동로그인 기능 적용 시 사용
//        String refreshToken = jwtTokenProvider.createRtToken(user, dto.isRememberMe());

        return new UserLoginResDto(accessToken, refreshToken);
    }



}
