package com.Dolmeng_E.drive.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // STOMP를 사용하기 위해 추가
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 클라이언트가 WebSocket 연결을 시작할 엔드포인트
        // 기존 /ws/editor 경로를 그대로 사용
        registry.addEndpoint("/ws/editor")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // STOMP 메시지의 destination prefix 설정
        // /app으로 시작하는 destination은 @MessageMapping이 붙은 메서드로 라우팅
        registry.setApplicationDestinationPrefixes("/publish");

        // /topic, /queue로 시작하는 destination을 가진 메시지를 브로커로 라우팅
        // 클라이언트는 이 경로를 구독(subscribe)하여 메시지를 수신
        registry.enableSimpleBroker("/topic");
    }
}