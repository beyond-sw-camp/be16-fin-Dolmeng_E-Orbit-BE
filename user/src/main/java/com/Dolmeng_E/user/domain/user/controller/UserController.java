package com.Dolmeng_E.user.domain.user.controller;

import com.Dolmeng_E.user.domain.user.dto.UserInfoResDto;
import com.Dolmeng_E.user.domain.user.entity.User;
import com.Dolmeng_E.user.domain.user.dto.UserCreateReqDto;
import com.Dolmeng_E.user.domain.user.dto.UserLoginReqDto;
import com.Dolmeng_E.user.domain.user.dto.UserLoginResDto;
import com.Dolmeng_E.user.domain.user.service.UserService;
import com.Dolmeng_E.user.common.auth.JwtTokenProvider;
import com.Dolmeng_E.user.common.dto.CommonSuccessDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/create")
    public ResponseEntity<?> create(@ModelAttribute @Valid UserCreateReqDto dto) {
        userService.create(dto);
        return new ResponseEntity<>(new CommonSuccessDto(dto.getEmail(), HttpStatus.CREATED.value(), "회원가입 성공"), HttpStatus.CREATED);
    }

    @PostMapping("/login")
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


}
