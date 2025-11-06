package com.Dolmeng_E.user.domain.user.controller;

import com.Dolmeng_E.user.common.dto.UserEmailListDto;
import com.Dolmeng_E.user.common.dto.UserIdListDto;
import com.Dolmeng_E.user.common.dto.UserInfoListResDto;
import com.Dolmeng_E.user.common.dto.UserInfoResDto;
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

import java.util.UUID;

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

    // 유저 ID, 이름, 이메일 반환 API
    @GetMapping("/return")
    public UserInfoResDto fetchUserInfo(@RequestHeader("X-User-Id")String userId) {
        UserInfoResDto userInfoResDto = userService.fetchUserInfo(userId);
        return userInfoResDto;
    }

    // 유저 ID, 이름, 이메일, 유저 프로필url 반환 API
    @GetMapping("/return/by-id")
    UserInfoResDto fetchUserInfoById(@RequestHeader("X-User-Id")String userId) {
        UserInfoResDto userInfoResDto = userService.fetchUserInfoById(userId);
        return userInfoResDto;
    }

    // 유저 ID, 이름, 이메일, 유저 프로필url 반환 API
    @PostMapping("/return/by-email")
    UserInfoListResDto fetchUserInfoByEmail(@RequestBody UserEmailListDto userEmailListDto) {
        UserInfoListResDto userInfoListResDto = userService.fetchUserInfoByEmail(userEmailListDto);
        return userInfoListResDto;
    }

    // 유저 정보 list 반환 API
    @PostMapping("/return/users")
    UserInfoListResDto fetchUserListInfo(@RequestBody UserIdListDto userIdListDto) {
        UserInfoListResDto userInfoListResDto = userService.fetchUserListInfo(userIdListDto);
        return userInfoListResDto;
    }

    // 모든 유저 정보 list 반환 API
    @GetMapping("/return/all-users")
    UserInfoListResDto fetchAllUserListInfo(@RequestHeader("X-User-Id")String userId) {
        UserInfoListResDto userInfoListResDto = userService.fetchAllUserListInfo(userId);
        return userInfoListResDto;
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
    @PostMapping("/new-user")
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
    public ResponseEntity<?> logout(@RequestHeader("X-User-Id")String userId) {
        userService.logout(userId);
        return new ResponseEntity<>(new CommonSuccessDto(userId, HttpStatus.OK.value(), "로그아웃 성공"),  HttpStatus.OK);
    }

    // 회원 탈퇴 API
    @DeleteMapping("/auth")
    public ResponseEntity<?> delete(@RequestHeader("X-User-Id")String userId) {
        userService.delete(userId);
        return new ResponseEntity<>(new CommonSuccessDto(userId, HttpStatus.OK.value(), "회원탈퇴 성공"),  HttpStatus.OK);
    }

    // 회원정보 조회 API
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserDetail(@PathVariable("userId") UUID userId) {
        UserDetailResDto userDetailResDto = userService.getUserDetail(userId);
        return new ResponseEntity<>(new CommonSuccessDto(userDetailResDto, HttpStatus.OK.value(), "회원상세 조회 성공"),  HttpStatus.OK);
    }

    // 회원 정보 수정
    @PutMapping("/auth")
    public ResponseEntity<?> update(@ModelAttribute @Valid UserUpdateReqDto dto, @RequestHeader("X-User-Id")String userId) {
        userService.update(dto, userId);
        return new ResponseEntity<>(new CommonSuccessDto(userId, HttpStatus.OK.value(), "회원정보 수정 성공"), HttpStatus.OK);
    }

    // 비밀번호 리셋 API 구현1 - 이메일 검증
    @PostMapping("/password/email")
    public ResponseEntity<?> verifyEmailForPasswordReset(@RequestBody @Valid UserEmailReqDto dto) {
        userService.verifyEmailForPasswordReset(dto);
        return new ResponseEntity<>(new CommonSuccessDto(dto.getEmail(), HttpStatus.OK.value(), "이메일 존재여부 검증 및 인증코드 전송 성공"), HttpStatus.OK);
    }

    // 비밀번호 리셋 API 구현2 - 인증코드 검증
    @PostMapping("/password/authcode")
    public ResponseEntity<?> verifyAuthCodeForPassword(@RequestBody @Valid UserEmailAuthCodeReqDto dto) {
        userService.verifyAuthCodeForPassword(dto);
        return new ResponseEntity<>(new CommonSuccessDto(dto.getEmail(), HttpStatus.OK.value(), "인증코드 검증 성공"), HttpStatus.OK);
    }

    // 비밀번호 리셋 API 구현3 - 비밀번호 리셋
    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(@RequestBody @Valid UserUpdatePasswordReqDto dto) {
        userService.updatePassword(dto);
        return new ResponseEntity<>(new CommonSuccessDto(dto.getEmail(), HttpStatus.OK.value(), "비밀번호 수정 성공"), HttpStatus.OK);
    }

    // 회원 검색
    @PostMapping("/search")
    public ResponseEntity<?> searchUser(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody @Valid SearchDto dto
    ) {
        UserInfoListResDto userInfoListResDto = userService.searchUser(userId, dto);

        return new ResponseEntity<>(
                CommonSuccessDto.builder()
                        .result(userInfoListResDto)
                        .statusCode(HttpStatus.OK.value())
                        .statusMessage("회원 검색 성공")
                        .build(),
                HttpStatus.OK
        );
    }

    // 아직 초대되지 않은 사용자 목록 반환 API
    @PostMapping("/not-in-workspace")
    public UserInfoListResDto getUsersNotInWorkspace(@RequestBody UserIdListDto dto) {
        return userService.getUsersNotInIds(dto.getUserIdList());
    }

}
