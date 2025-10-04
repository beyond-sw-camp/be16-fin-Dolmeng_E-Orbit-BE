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

    // 회원가입 API
    @PostMapping
    public ResponseEntity<?> create(@ModelAttribute @Valid UserCreateReqDto dto) {
        userService.create(dto);
        return new ResponseEntity<>(new CommonSuccessDto(dto.getEmail(), HttpStatus.CREATED.value(), "회원가입 성공"), HttpStatus.CREATED);
    }

    // 로그인 API
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody @Valid UserLoginReqDto dto) {
        User user = userService.login(dto);

        String accessToken = jwtTokenProvider.createAtToken(user);
        String refreshToken = jwtTokenProvider.createRtToken(user);
        return new ResponseEntity<>(new CommonSuccessDto(new UserLoginResDto(accessToken, refreshToken), HttpStatus.OK.value(), "로그인 성공"), HttpStatus.OK);
    }

    // 유저 ID, 이름 반환 API
    @GetMapping("/return")
    public UserInfoResDto fetchUserInfo(@RequestHeader("X-User-Email")String userEmail) {
        UserInfoResDto userInfoResDto = userService.fetchUserInfo(userEmail);
        return userInfoResDto;
    }

    // 카카오 로그인 (정보 없으면 회원가입까지)
    @PostMapping("/kakao/login")
    public ResponseEntity<?> kakaoLogin(@RequestBody RedirectDto dto) {
        UserLoginResDto userLoginResDto = userService.kakaoLogin(dto);

        String accessToken = userLoginResDto.getAccessToken();
        String refreshToken = userLoginResDto.getRefreshToken();
        return new ResponseEntity<>(new CommonSuccessDto(new UserLoginResDto(accessToken, refreshToken), HttpStatus.OK.value(), "kakao 연동 성공"), HttpStatus.OK);
    }

}
