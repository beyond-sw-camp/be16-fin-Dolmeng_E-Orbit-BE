package com.Dolmeng_E.user.domain.user.service;

import com.Dolmeng_E.user.domain.user.dto.AccessTokenDto;
import com.Dolmeng_E.user.domain.user.dto.KakaoProfileDto;
import com.Dolmeng_E.user.domain.user.entity.SocialType;
import com.Dolmeng_E.user.domain.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
public class KakaoService {

    @Value("${oauth.kakao.client-id}")
    private String kakaoClientId;

    @Value("${oauth.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    public User createOauth(KakaoProfileDto kakaoProfileDto) {
        User user = User.builder()
                .name(kakaoProfileDto.getKakao_account().getProfile().getNickname())
                .email(kakaoProfileDto.getKakao_account().getEmail())
                .socialType(SocialType.KAKAO)
                .socialId(kakaoProfileDto.getId())
                .profileImageUrl(kakaoProfileDto.getKakao_account().getProfile().getProfile_image_url())
                .build();
        return user;
    }

    public AccessTokenDto getAccessToken(String code) {
        RestClient restClient = RestClient.create();

        // MultiValueMap을 통해 자동으로 form-data 형식으로 body 조립 가능
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", kakaoClientId);
        params.add("redirect_uri", kakaoRedirectUri);
        params.add("grant_type", "authorization_code");

        ResponseEntity<AccessTokenDto> response = restClient.post()
                .uri("https://kauth.kakao.com/oauth/token")
                .header("Content-Type", "application/x-www-form-urlencoded")

                .body(params)

                .retrieve()
                .toEntity(AccessTokenDto.class);

        return response.getBody();
    }

    public KakaoProfileDto getKakaoProfile(String token) {
        RestClient restClient = RestClient.create();

        ResponseEntity<KakaoProfileDto> response = restClient.post()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header("Authorization", "Bearer "+token)
                .retrieve()
                .toEntity(KakaoProfileDto.class);

        System.out.println("profile json" + response.getBody());

        return response.getBody();
    }
}
