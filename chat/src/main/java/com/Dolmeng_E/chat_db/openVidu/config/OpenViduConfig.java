package com.Dolmeng_E.chat_db.openVidu.config;

import io.openvidu.java.client.OpenVidu;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OpenViduConfig {
    // application.yml 파일의 openvidu.url 값을 주입
    @Value("${openvidu.url}")
    private String openViduUrl;

    // application.yml 파일의 openvidu.secret 값을 주입
    @Value("${openvidu.secret}")
    private String openViduSecret;

    @Bean
    public OpenVidu openVidu() {
        return new OpenVidu(openViduUrl, openViduSecret);
    }
}

