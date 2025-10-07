package com.Dolmeng_E.user.domain.user.service;

import com.Dolmeng_E.user.common.auth.JwtTokenProvider;
import com.Dolmeng_E.user.domain.user.dto.*;
import com.Dolmeng_E.user.domain.user.entity.User;
import com.Dolmeng_E.user.domain.user.repository.UserRepository;
import com.example.modulecommon.service.S3Uploader;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3Uploader s3Uploader;
    private final JwtTokenProvider jwtTokenProvider;
    private final KakaoService kakaoService;
    private final GoogleService googleService;
    private final MailService mailService;
    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, S3Uploader s3Uploader, JwtTokenProvider jwtTokenProvider, KakaoService kakaoService, GoogleService googleService, MailService mailService, @Qualifier("rtInventory") RedisTemplate<String, String> redisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.s3Uploader = s3Uploader;
        this.jwtTokenProvider = jwtTokenProvider;
        this.kakaoService = kakaoService;
        this.googleService = googleService;
        this.mailService = mailService;
        this.redisTemplate = redisTemplate;
    }

    // 회원가입 API 구현3 - 계정 생성
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
        String refreshToken = jwtTokenProvider.createRtToken(user, dto.isRememberMe());

        return new UserLoginResDto(accessToken, refreshToken);
    }

    // 구글 로그인 API (정보 없으면 회원가입까지)
    public UserLoginResDto googleLogin(RedirectDto dto) {

        // accessToken 발급
        AccessTokenDto accessTokenDto = googleService.getAccessToken(dto.getCode());
        // 사용자 정보 얻기
        GoogleProfileDto googleProfileDto = googleService.getGoogleProfile(accessTokenDto.getAccess_token());

        // 회원가입이 되어있지 않다면 회원가입
        User user = userRepository.findBySocialId(googleProfileDto.getSub()).orElse(null);
        if(user == null) {
            user = googleService.createOauth(googleProfileDto);
            userRepository.save(user);
        }
        if(user.isDelete()) throw new IllegalArgumentException("탈퇴한 회원입니다.");

        // 토큰 생성해서 반환
        String accessToken = jwtTokenProvider.createAtToken(user);
        String refreshToken = jwtTokenProvider.createRtToken(user, dto.isRememberMe());

        return new UserLoginResDto(accessToken, refreshToken);
    }

    // 회원가입 API 구현1 - 이메일 입력 단계 - 중복 검증 및 이메일 인증코드 전송
    public void sendSignupVerificationCode(UserEmailReqDto dto) {
        if(userRepository.findByEmail(dto.getEmail()).isPresent()) throw new EntityExistsException("중복되는 이메일입니다.");

        // 중복문제 없으니, 이메일 인증코드 전송
        String authCode = mailService.sendMimeMessage(dto.getEmail());

        // auth code -> redis에 저장
        redisTemplate.opsForValue().set("EmailAuthCode:" + dto.getEmail(), authCode, 3, TimeUnit.MINUTES);
    }

    // 회원가입 API 구현2 - 인증코드 검증 단계
    public void verifyAuthCode(UserEmailAuthCodeReqDto dto) {
        String authCode = redisTemplate.opsForValue().get("EmailAuthCode:" + dto.getEmail());
        if(authCode == null) { throw new RuntimeException("인증코드가 누락되었습니다."); }

        // 인증코드 불일치 시, 예외 발생
        if(!authCode.equals(dto.getAuthCode())) {
            throw new IllegalArgumentException("인증코드가 다릅니다.");
        }
    }

    // access/refresh token 갱신 API
    public User tokenRefresh(UserRefreshTokenReqDto dto) {
        User user = jwtTokenProvider.validateRt(dto.getRefreshToken());
        return user;
    }

    // 로그아웃 API
    public void logout(String userEmail) {
        userRepository.findByEmail(userEmail).orElseThrow(() -> new EntityNotFoundException("없는 회원입니다."));
        jwtTokenProvider.removeRt(userEmail);
    }

    // 회원 탈퇴 API
    public void delete(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new EntityNotFoundException("없는 회원입니다."));
        user.updateDeleted(true);
    }

    // 회원정보 조회 API
    public UserDetailResDto getUserDetail(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("없는 회원입니다."));
        return UserDetailResDto.fromEntity(user);
    }

    // 회원 정보 수정
    public void update(UserUpdateReqDto dto, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new EntityNotFoundException("없는 회원입니다."));
        if(dto.getName() != null && !dto.getName().isEmpty()) { user.updateName(dto.getName()); }
        if(dto.getPhoneNumber() != null) { user.updatePhoneNumber(dto.getPhoneNumber()); }
        if(dto.getProfileImage() != null) {
            s3Uploader.delete(user.getProfileImageUrl());
            String profileImgaeUrl = s3Uploader.upload(dto.getProfileImage(), "user");
            user.updateProfileImageUrl(profileImgaeUrl);
        }
    }

    // 비밀번호 리셋 API 구현1 - 이메일 검증
    public void verifyEmailForPasswordReset(UserEmailReqDto dto) {
        userRepository.findByEmail(dto.getEmail()).orElseThrow(() -> new EntityNotFoundException("없는 회원입니다."));

        String authCode = mailService.sendMimeMessage(dto.getEmail());

        redisTemplate.opsForValue().set("PasswordAuthCode:" + dto.getEmail(), authCode, 3, TimeUnit.MINUTES);
    }

    // 비밀번호 리셋 API 구현2 - 인증코드 검증
    public void verifyAuthCodeForPassword(UserEmailAuthCodeReqDto dto) {
        String authCode = redisTemplate.opsForValue().get("PasswordAuthCode:" + dto.getEmail());
        if(authCode == null) { throw new RuntimeException("인증코드가 누락되었습니다."); }

        // 인증코드 불일치 시, 예외 발생
        if(!authCode.equals(dto.getAuthCode())) {
            throw new IllegalArgumentException("인증코드가 다릅니다.");
        }
    }

    // 비밀번호 리셋 API 구현3 - 비밀번호 리셋
    public void updatePassword(UserUpdatePasswordReqDto dto) {
        User user = userRepository.findByEmail(dto.getEmail()).orElseThrow(()->new EntityNotFoundException("없는 회원입니다."));
        String encodedPassword = passwordEncoder.encode(dto.getNewPassword());
        user.updatePassword(encodedPassword);
    }
}
