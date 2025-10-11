package com.Dolmeng_E.chat.common.config;

import com.Dolmeng_E.chat.common.service.JwtParserUtil;
import com.Dolmeng_E.chat.domain.service.ChatService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class StompEventListener {
    @Value("${jwt.secretKeyAt}")
    private String secretKeyAt;

    private final Set<String> sessions = ConcurrentHashMap.newKeySet();
    private final RedisTemplate<String, String> redisTemplate;
    private final ChatService chatService;
    private final JwtParserUtil jwtParserUtil;

    private Key secret_at_key;

    public StompEventListener(@Qualifier("rtInventory") RedisTemplate<String, String> redisTemplate, ChatService chatService, JwtParserUtil jwtParserUtil) {
        this.redisTemplate = redisTemplate;
        this.chatService = chatService;
        this.jwtParserUtil = jwtParserUtil;
    }

    @PostConstruct
    public void init() {
        secret_at_key = new SecretKeySpec(java.util.Base64.getDecoder().decode(secretKeyAt), SignatureAlgorithm.HS512.getJcaName());
    }

    @EventListener
    public void connectHandle(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        sessions.add(accessor.getSessionId());
        System.out.println("connectHandle() - total session: " + sessions.size());
    }

    @EventListener
    public void subscribeHandle(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        String destination = accessor.getDestination();  // /topic/123
        String bearerToken = accessor.getFirstNativeHeader("Authorization");
        String sessionId = accessor.getSessionId();

        if (bearerToken == null || destination == null) return;

        String accessToken = bearerToken.substring(7);

        // 검증 없이 email만 추출
        String email = jwtParserUtil.extractEmailWithoutValidation(accessToken);

        // destination에서 roomId 추출
        String[] parts = destination.split("/");
        String roomId = parts[parts.length - 1];

        // room의 권한 체크 후 참여한 채팅방의 모든 메시지 읽음 처리
        if(!chatService.isRoomParticipant(email, Long.parseLong(roomId))) {
            throw new IllegalArgumentException("해당 room에 대한 권한이 없습니다.");
        }
        chatService.messageRead(Long.parseLong(roomId), email);

        log.info("subscribeHandle() - 입장 roomId: {}, email: {}, session: {}", roomId, email, sessionId);

        redisTemplate.opsForSet().add("chat:room:" + roomId + ":users", email);
        redisTemplate.opsForHash().put("chat:session:" + sessionId, "email", email);
        redisTemplate.opsForHash().put("chat:session:" + sessionId, "roomId", roomId);
    }

    @EventListener
    public void disconnectHandle(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        sessions.remove(accessor.getSessionId());
        log.info("disconnectHandle() - total session: " + sessions.size());

        String sessionId = accessor.getSessionId();

        // 세션에 저장된 정보 조회
        Map<Object, Object> sessionInfo = redisTemplate.opsForHash().entries("chat:session:" + sessionId);
        if (sessionInfo.isEmpty()) return;

        String email = (String) sessionInfo.get("email");
        String roomId = (String) sessionInfo.get("roomId");

        if (email != null && roomId != null) {
            redisTemplate.opsForSet().remove("chat:room:" + roomId + ":users", email);
            log.info("disconnectHandle() - 퇴장 roomId: {}, email: {}, session: {}", roomId, email, sessionId);
        }

        // 세션 데이터 정리
        redisTemplate.delete("chat:session:" + sessionId);
    }
}
