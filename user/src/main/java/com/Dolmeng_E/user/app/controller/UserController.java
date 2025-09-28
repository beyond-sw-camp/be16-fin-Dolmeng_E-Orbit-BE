package com.Dolmeng_E.user.app.controller;

import com.Dolmeng_E.user.app.domain.User;
import com.Dolmeng_E.user.app.dto.UserCreateReqDto;
import com.Dolmeng_E.user.app.dto.UserLoginReqDto;
import com.Dolmeng_E.user.app.dto.UserLoginResDto;
import com.Dolmeng_E.user.app.service.UserService;
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
}
