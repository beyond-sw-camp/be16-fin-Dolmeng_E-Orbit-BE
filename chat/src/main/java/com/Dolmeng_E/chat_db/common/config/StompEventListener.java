package com.Dolmeng_E.chat_db.common.config;

import com.Dolmeng_E.chat_db.common.service.JwtParserUtil;
import com.Dolmeng_E.chat_db.domain.service.ChatService;
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
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

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

        String destination = accessor.getDestination();  // 예: /topic/123 또는 /topic/summary/hong1@naver.com
        String bearerToken = accessor.getFirstNativeHeader("Authorization");
        String sessionId = accessor.getSessionId();

        if (bearerToken == null || destination == null) return;

        String accessToken = bearerToken.substring(7);
        String userId = jwtParserUtil.extractIdWithoutValidation(accessToken);

        // summary 구독이면 검증 로직 스킵
        if (destination.startsWith("/topic/summary")) {
            log.info("subscribeHandle() - summary 구독 감지 (userId: {}, session: {})", userId, sessionId);
            return;
        }

        // 채팅방 구독 (/topic/{roomId}) 처리
        String[] parts = destination.split("/");
        String roomIdStr = parts[parts.length - 1];

        try {
            Long roomId = Long.parseLong(roomIdStr);

            // room 권한 검증 및 읽음 처리
            if (!chatService.isRoomParticipant(userId, roomId)) {
                throw new IllegalArgumentException("해당 room에 대한 권한이 없습니다.");
            }
            chatService.messageRead(roomId, userId);

            log.info("subscribeHandle() - 입장 roomId: {}, userId: {}, session: {}", roomId, userId, sessionId);

            // Redis 저장
            redisTemplate.opsForSet().add("chat:room:" + roomId + ":users", userId);
            redisTemplate.opsForHash().put("chat:session:" + sessionId, "userId", userId);
            redisTemplate.opsForHash().put("chat:session:" + sessionId, "roomId", roomId);

        } catch (NumberFormatException e) {
            log.warn("subscribeHandle() - 잘못된 roomId 형식: {}, destination: {}", roomIdStr, destination);
        }
    }

    @EventListener
    public void unsubscribeHandle(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        Map<Object, Object> sessionInfo = redisTemplate.opsForHash().entries("chat:session:" + sessionId);
        if (sessionInfo.isEmpty()) return;

        log.info("unsubscribeHandle() - sessionId: {}, sessionInfo: {}", sessionId, sessionInfo);

        String userId = (String) sessionInfo.get("userId");
        Object roomIdObj = sessionInfo.get("roomId");
        String roomId = (roomIdObj instanceof Long) ? String.valueOf(roomIdObj)
                : (String) roomIdObj;

        if (userId != null && roomId != null) {
            redisTemplate.opsForSet().remove("chat:room:" + roomId + ":users", userId);
            log.info("unsubscribeHandle() - 구독 해제 roomId: {}, userId: {}", roomId, userId);
        }
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

        String userId = (String) sessionInfo.get("userId");
        Object roomIdObj = sessionInfo.get("roomId");
        String roomId = (roomIdObj instanceof Long) ? String.valueOf(roomIdObj)
                : (String) roomIdObj;

        System.out.println("disconnectHandle() - sessionId: " + sessionId + ", roomId: " + roomId + ", userId: " + userId);

        if (userId != null && roomId != null) {
            redisTemplate.opsForSet().remove("chat:room:" + roomId + ":users", userId);
            log.info("disconnectHandle() - roomId: {}, userId: {}, session: {}", roomId, userId, sessionId);
        }

        // 세션 데이터 정리
        redisTemplate.delete("chat:session:" + sessionId);
    }
}
