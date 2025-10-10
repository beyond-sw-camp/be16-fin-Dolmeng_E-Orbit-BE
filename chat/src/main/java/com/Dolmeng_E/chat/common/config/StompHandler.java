package com.Dolmeng_E.chat.common.config;

import com.Dolmeng_E.chat.domain.service.ChatService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {
    private final ChatService chatService;

    @Value("${jwt.secretKeyAt}")
    private String secretKeyAt;

    private Key secret_at_key;

    @PostConstruct
    public void init() {
        secret_at_key = new SecretKeySpec(java.util.Base64.getDecoder().decode(secretKeyAt), SignatureAlgorithm.HS512.getJcaName());
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        final StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if(StompCommand.CONNECT == accessor.getCommand()){
            System.out.println("connect요청 시 토큰 유효성 검증");
            String bearerToken = accessor.getFirstNativeHeader("Authorization");
            String token = bearerToken.substring(7);

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secret_at_key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String email = claims.getSubject();

            System.out.println("토큰 검증 완료");
        }
        if(StompCommand.SUBSCRIBE == accessor.getCommand()){
            System.out.println("subscribe 요청 시 토큰 검증 및 참여자 검증");
            String bearerToken = accessor.getFirstNativeHeader("Authorization");
            String token = bearerToken.substring(7);

            System.out.println("bearerToken: " + bearerToken);

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secret_at_key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String email = claims.getSubject();
            String roomId = accessor.getDestination().split("/")[2];

            if(!chatService.isRoomParticipant(email, Long.parseLong(roomId))) {
                throw new IllegalArgumentException("해당 room에 대한 권한이 없습니다.");
            }
            System.out.println("subscribe 검증 완료");
        }

        return message;
    }

}
