package com.Dolmeng_E.user.domain.user.service;

import com.Dolmeng_E.user.domain.user.dto.AccessTokenDto;
import com.Dolmeng_E.user.domain.user.dto.GoogleProfileDto;
import com.Dolmeng_E.user.domain.user.entity.SocialType;
import com.Dolmeng_E.user.domain.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
public class GoogleService {

    @Value("${oauth.google.client-id}")
    private String googleClientId;

    @Value("${oauth.google.client-secret}")
    private String googleClientSecret;

    @Value("${oauth.google.redirect-uri}")
    private String googleRedirectUri;

    public User createOauth(GoogleProfileDto googleProfileDto) {
        User user = User.builder()
                .name(googleProfileDto.getName())
                .email(googleProfileDto.getEmail())
                .socialType(SocialType.GOOGLE)
                .socialId(googleProfileDto.getSub())
                .profileImageUrl(googleProfileDto.getPicture())
                .build();
        return user;
    }

    public AccessTokenDto getAccessToken(String code) {
        RestClient restClient = RestClient.create();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("redirect_uri", googleRedirectUri);
        params.add("grant_type", "authorization_code");

        ResponseEntity<AccessTokenDto> response = restClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(params)
                .retrieve()
                .toEntity(AccessTokenDto.class);

        return response.getBody();
    }

    public GoogleProfileDto getGoogleProfile(String token) {
        RestClient restClient = RestClient.create();

        ResponseEntity<GoogleProfileDto> response = restClient.post()
                .uri("https://openidconnect.googleapis.com/v1/userinfo")
                .header("Authorization", "Bearer "+token)
                .retrieve()
                .toEntity(GoogleProfileDto.class);

        System.out.println("profile json" + response.getBody());

        return response.getBody();
    }
}
