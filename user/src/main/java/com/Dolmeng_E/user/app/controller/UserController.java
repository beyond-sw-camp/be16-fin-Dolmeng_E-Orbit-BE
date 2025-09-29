package com.Dolmeng_E.user.app.controller;

import com.Dolmeng_E.user.app.dto.UserCreateResDto;
import com.Dolmeng_E.user.app.service.UserService;
import com.Dolmeng_E.user.common.dto.CommonSuccessDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    @PostMapping("/create")
    public ResponseEntity<?> create(@ModelAttribute UserCreateResDto dto) {
        userService.create(dto);
        return new ResponseEntity<>(new CommonSuccessDto(dto.getEmail(), HttpStatus.CREATED.value(), "회원가입 성공"), HttpStatus.CREATED);
    }
}
