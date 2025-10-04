package com.Dolmeng_E.user.domain.user.controller;

import com.Dolmeng_E.user.domain.user.dto.*;
import com.Dolmeng_E.user.domain.user.entity.User;
import com.Dolmeng_E.user.domain.user.service.UserService;
import com.Dolmeng_E.user.common.auth.JwtTokenProvider;
import com.example.modulecommon.dto.CommonSuccessDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    // 로그인 API
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody @Valid UserLoginReqDto dto) {
        User user = userService.login(dto);

        String accessToken = jwtTokenProvider.createAtToken(user);
        String refreshToken = jwtTokenProvider.createRtToken(user, dto.isRememberMe());
        return new ResponseEntity<>(new CommonSuccessDto(new UserLoginResDto(accessToken, refreshToken), HttpStatus.OK.value(), "로그인 성공"), HttpStatus.OK);
    }

    // 유저 ID, 이름 반환 API
    @GetMapping("/return")
    public UserInfoResDto fetchUserInfo(@RequestHeader("X-User-Email")String userEmail) {
        UserInfoResDto userInfoResDto = userService.fetchUserInfo(userEmail);
        return userInfoResDto;
    }

    // 카카오 로그인 API (정보 없으면 회원가입까지)
    @PostMapping("/kakao/login")
    public ResponseEntity<?> kakaoLogin(@RequestBody RedirectDto dto) {
        UserLoginResDto userLoginResDto = userService.kakaoLogin(dto);

        String accessToken = userLoginResDto.getAccessToken();
        String refreshToken = userLoginResDto.getRefreshToken();

        return new ResponseEntity<>(new CommonSuccessDto(new UserLoginResDto(accessToken, refreshToken), HttpStatus.OK.value(), "kakao 로그인 성공"), HttpStatus.OK);
    }

    // 구글 로그인 (정보 없으면 회원가입까지)
    @PostMapping("/google/login")
    public ResponseEntity<?> googleLogin(@RequestBody RedirectDto dto) {
        UserLoginResDto userLoginResDto = userService.googleLogin(dto);

        String accessToken = userLoginResDto.getAccessToken();
        String refreshToken = userLoginResDto.getRefreshToken();

        return new ResponseEntity<>(new CommonSuccessDto(new UserLoginResDto(accessToken, refreshToken), HttpStatus.OK.value(), "google 로그인 성공"), HttpStatus.OK);
    }

    // 회원가입 API 구현1 - 이메일 입력 단계 - 중복 검증 및 이메일 인증코드 전송
    @PostMapping("/email")
    public ResponseEntity<?> sendSignupVerificationCode(@RequestBody @Valid UserEmailReqDto dto) {
        userService.sendSignupVerificationCode(dto);
        return new ResponseEntity<>(new CommonSuccessDto(dto.getEmail(), HttpStatus.OK.value(), "이메일 중복 검증 및 인증코드 전송 성공"), HttpStatus.OK);
    }

    // 회원가입 API 구현2 - 인증코드 검증 단계
    @PostMapping("/authcode")
    public ResponseEntity<?> verifyAuthCode(@RequestBody @Valid UserEmailAuthCodeReqDto dto) {
        userService.verifyAuthCode(dto);
        return new ResponseEntity<>(new CommonSuccessDto(dto.getEmail(), HttpStatus.OK.value(), "인증코드 검증 성공"), HttpStatus.OK);
    }

    // 회원가입 API 구현3 - 회원가입 완료(프로필 사진, 이름, 전화번호, 비밀번호)
    @PostMapping
    public ResponseEntity<?> create(@ModelAttribute @Valid UserCreateReqDto dto) {
        userService.create(dto);
        return new ResponseEntity<>(new CommonSuccessDto(dto.getEmail(), HttpStatus.CREATED.value(), "회원가입 성공"), HttpStatus.CREATED);
    }
  
    // access/refresh token 갱신 API
    @PostMapping("/auth/token")
    public ResponseEntity<?> tokenRefresh(@RequestBody @Valid UserRefreshTokenReqDto dto) {
        User user = userService.tokenRefresh(dto);
        String accessToken = jwtTokenProvider.createAtToken(user);
        String refreshToken = jwtTokenProvider.createRtToken(user, dto.isRememberMe());
        return new ResponseEntity<>(new CommonSuccessDto(new UserLoginResDto(accessToken, refreshToken), HttpStatus.OK.value(), "access/refresh token 재발급 성공"), HttpStatus.OK);
    }

    // 로그아웃 API
    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(@RequestHeader("X-User-Email")String userEmail) {
        userService.logout(userEmail);
        return new ResponseEntity<>(new CommonSuccessDto(userEmail, HttpStatus.OK.value(), "로그아웃 성공"),  HttpStatus.OK);
    }

    // 회원 탈퇴 API
    @DeleteMapping("/auth")
    public ResponseEntity<?> delete(@RequestHeader("X-User-Email")String userEmail) {
        userService.delete(userEmail);
        return new ResponseEntity<>(new CommonSuccessDto(userEmail, HttpStatus.OK.value(), "회원탈퇴 성공"),  HttpStatus.OK);
    }
}
