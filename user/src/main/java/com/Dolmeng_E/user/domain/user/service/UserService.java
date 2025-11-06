package com.Dolmeng_E.user.domain.user.service;

import com.Dolmeng_E.user.common.auth.JwtTokenProvider;
import com.Dolmeng_E.user.common.dto.UserEmailListDto;
import com.Dolmeng_E.user.common.dto.UserIdListDto;
import com.Dolmeng_E.user.common.dto.UserInfoListResDto;
import com.Dolmeng_E.user.common.dto.UserInfoResDto;
import com.Dolmeng_E.user.common.service.S3Uploader;
import com.Dolmeng_E.user.common.service.UserSignupOrchestrationService;
import com.Dolmeng_E.user.domain.user.dto.*;
import com.Dolmeng_E.user.domain.user.entity.User;
import com.Dolmeng_E.user.domain.user.repository.UserRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    private final UserSignupOrchestrationService userSignupOrchestrationService;
    private final HashOperations<String, String, String> hashOperations;

    @Autowired
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder, S3Uploader s3Uploader,
                       JwtTokenProvider jwtTokenProvider,
                       KakaoService kakaoService, GoogleService googleService,
                       MailService mailService, @Qualifier("rtInventory") RedisTemplate<String, String> redisTemplate,
                       UserSignupOrchestrationService userSignupOrchestrationService,
                       @Qualifier("userInventory") RedisTemplate<String, String> userRedisTemplate
                       ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.s3Uploader = s3Uploader;
        this.jwtTokenProvider = jwtTokenProvider;
        this.kakaoService = kakaoService;
        this.googleService = googleService;
        this.mailService = mailService;
        this.redisTemplate = redisTemplate;
        this.userSignupOrchestrationService = userSignupOrchestrationService;
        this.hashOperations = userRedisTemplate.opsForHash();
    }

    // 회원가입 API 구현3 - 계정 생성
    public void create(UserCreateReqDto dto) {
        if(userRepository.findByEmail(dto.getEmail()).isPresent()) throw new EntityExistsException("중복되는 이메일입니다.");

        String encodedPassword = passwordEncoder.encode(dto.getPassword());
        String profileImgaeUrl = null;
        if(dto.getProfileImageUrl() != null && !dto.getProfileImageUrl().isEmpty()) {
            profileImgaeUrl = s3Uploader.upload(dto.getProfileImageUrl(), "user");
        }

        User user = dto.toEntity(encodedPassword, profileImgaeUrl);
        User saveUser = userRepository.save(user);

        // 회원가입 시 redis에 저장
        saveUserInfoToRedis(saveUser);

        // 회원가입 시 워크스페이스 생성 메세지 발송
        userSignupOrchestrationService.publishSignupEvent(user);

    }

    // 로그인 API
    public User login(UserLoginReqDto dto) {
        User user = userRepository.findByEmail(dto.getEmail()).orElseThrow(() -> new EntityNotFoundException("없는 회원입니다."));
        if(!passwordEncoder.matches(dto.getPassword(), user.getPassword())) throw new IllegalArgumentException("비밀번호가 틀렸습니다.");
        return user;
    }

    // 유저 ID, 이름, email, 유저 프로필 url 반환 API
    public UserInfoResDto fetchUserInfo(String userId) {
        User user = userRepository.findById(UUID.fromString(userId)).orElseThrow(()->new EntityNotFoundException("없는 회원입니다."));
        return UserInfoResDto.builder()
                .userId(user.getId())
                .userName(user.getName())
                .userEmail(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .build();

    }
    public UserInfoResDto fetchUserInfoById(String userId) {
        User user = userRepository.findById(UUID.fromString(userId)).orElseThrow(()->new EntityNotFoundException("없는 회원입니다."));
        return UserInfoResDto.builder()
                .userId(user.getId())
                .userName(user.getName())
                .userEmail(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }

    public UserInfoListResDto fetchUserInfoByEmail(UserEmailListDto userEmailListDto) {
        List<UserInfoResDto> userInfoList = new ArrayList<>();

        for (String email : userEmailListDto.getUserEmailList()) {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 이메일입니다: " + email));

            UserInfoResDto userInfo = UserInfoResDto.builder()
                    .userId(user.getId())
                    .userName(user.getName())
                    .userEmail(user.getEmail())
                    .profileImageUrl(user.getProfileImageUrl())
                    .build();

            userInfoList.add(userInfo);
        }

        return UserInfoListResDto.builder()
                .userInfoList(userInfoList)
                .build();
    }

    // 유저 정보 list 반환 API
    public UserInfoListResDto fetchUserListInfo(UserIdListDto userIdListDto) {

        List<UserInfoResDto> userInfoList = new ArrayList<>();
        for (UUID userId : userIdListDto.getUserIdList()) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("없는 회원입니다."));

            // 유저 정보 DTO 생성
            UserInfoResDto userInfo = UserInfoResDto.builder()
                    .userId(user.getId())
                    .userName(user.getName())
                    .userEmail(user.getEmail())
                    .build();
            userInfoList.add(userInfo);
        }
        return UserInfoListResDto.builder()
                .userInfoList(userInfoList)
                .build();
    }

    // 모든 유저 정보 list 반환 API
    public UserInfoListResDto fetchAllUserListInfo(String userId) {

        // 1. 요청자 검증
        User requester = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("없는 회원입니다."));

        // 2. 전체 유저 조회
        List<User> allUserList = userRepository.findAll();

        // 3. DTO 변환
        List<UserInfoResDto> userInfoList = allUserList.stream()
                .map(user -> UserInfoResDto.builder()
                        .userId(user.getId())
                        .userName(user.getName())
                        .userEmail(user.getEmail())
                        .profileImageUrl(user.getProfileImageUrl())
                        .build())
                .toList();

        // 4. 반환
        return UserInfoListResDto.builder()
                .userInfoList(userInfoList)
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
            // 회원가입 시 redis에 저장
            saveUserInfoToRedis(user);
            // 회원가입 시 워크스페이스 생성 메세지 발송
            userSignupOrchestrationService.publishSignupEvent(user);
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
            // 회원가입 시 redis에 저장
            saveUserInfoToRedis(user);
            // 회원가입 시 워크스페이스 생성 메세지 발송
            userSignupOrchestrationService.publishSignupEvent(user);
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
    public void logout(String userId) {
        userRepository.findById(UUID.fromString(userId)).orElseThrow(() -> new EntityNotFoundException("없는 회원입니다."));
        jwtTokenProvider.removeRt(userId);
    }

    // 회원 탈퇴 API
    public void delete(String userId) {
        User user = userRepository.findById(UUID.fromString(userId)).orElseThrow(() -> new EntityNotFoundException("없는 회원입니다."));
        user.updateDeleted(true);
        hashOperations.put("user:"+user.getId(), "isDelete", String.valueOf(user.isDelete()));
    }

    // 회원정보 조회 API
    public UserDetailResDto getUserDetail(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("없는 회원입니다."));
        return UserDetailResDto.fromEntity(user);
    }

    // 회원 정보 수정
    public void update(UserUpdateReqDto dto, String userId) {
        User user = userRepository.findById(UUID.fromString(userId)).orElseThrow(() -> new EntityNotFoundException("없는 회원입니다."));
        if(dto.getName() != null && !dto.getName().isEmpty()) { user.updateName(dto.getName()); }
        if(dto.getPhoneNumber() != null) { user.updatePhoneNumber(dto.getPhoneNumber()); }
        if(dto.getProfileImage() != null) {
            if(user.getProfileImageUrl() != null) {
                s3Uploader.delete(user.getProfileImageUrl());
            }
            String profileImgaeUrl = s3Uploader.upload(dto.getProfileImage(), "user");
            user.updateProfileImageUrl(profileImgaeUrl);
        } else {
            user.updateProfileImageUrl(null);
        }

        // redis 정보 덮어쓰기
        saveUserInfoToRedis(user);
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

    // 회원 검색
    public UserInfoListResDto searchUser(String userId, SearchDto dto) {

        // 1. 요청자 검증
        userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new EntityNotFoundException("없는 회원입니다."));

        // 2. 검색 키워드
        String keyword = dto.getSearchKeyword();
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

        // 3. 이메일 또는 이름에 키워드가 포함된 유저 검색
        List<User> matchedUsers = userRepository.findByEmailContainingIgnoreCaseOrNameContainingIgnoreCase(keyword, keyword);

        // 4. DTO 변환
        List<UserInfoResDto> userInfoList = matchedUsers.stream()
                .map(user -> UserInfoResDto.builder()
                        .userId(user.getId())
                        .userName(user.getName())
                        .userEmail(user.getEmail())
                        .profileImageUrl(user.getProfileImageUrl())
                        .build())
                .toList();

        // 5. 결과 반환
        return UserInfoListResDto.builder()
                .userInfoList(userInfoList)
                .build();
    }

    // 아직 초대되지 않은 사용자 목록 반환 API
    public UserInfoListResDto getUsersNotInIds(List<UUID> excludedIds) {
        List<User> users = excludedIds == null || excludedIds.isEmpty()
                ? userRepository.findAll()
                : userRepository.findAllNotInIds(excludedIds);

        List<UserInfoResDto> userInfoList = users.stream()
                .map(UserInfoResDto::fromEntity)
                .collect(Collectors.toList());

        return UserInfoListResDto.builder()
                .userInfoList(userInfoList)
                .build();
    }

    // redis에 user 정보 저장
    public void  saveUserInfoToRedis(User user){
        String key = "user:" + user.getId();
        hashOperations.put(key, "id", String.valueOf(user.getId()));
        hashOperations.put(key, "email",user.getEmail());
        hashOperations.put(key, "name", user.getName() !=null ? user.getName() : "");
        hashOperations.put(key, "profileImageUrl",
                user.getProfileImageUrl() != null ? user.getProfileImageUrl() : "");
        hashOperations.put(key, "isDelete", String.valueOf(user.isDelete()));
    }
}
