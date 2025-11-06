package com.Dolmeng_E.user.common.auth;

import com.Dolmeng_E.user.domain.user.entity.User;
import com.Dolmeng_E.user.domain.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class JwtTokenProvider {
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.secretKeyAt}")
    private String secretKeyAt;

    @Value("${jwt.accessTokenExpiryMinutes}")
    private int accessTokenExpiryMinutes;

    @Value("${jwt.refreshTokenExpiryDaysNonPersistent}")
    private int refreshTokenExpiryDaysNonPersistent;
    @Value("${jwt.refreshTokenExpiryDaysPersistent}")
    private int refreshTokenExpiryDaysPersistent;

    @Value("${jwt.secretKeyRt}")
    private String secretKeyRt;

    private Key secret_at_key;
    private Key secret_rt_key;

    @Autowired
    public JwtTokenProvider(UserRepository userRepository, @Qualifier("rtInventory") RedisTemplate<String, String> redisTemplate) {
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        secret_at_key = new SecretKeySpec(java.util.Base64.getDecoder().decode(secretKeyAt), SignatureAlgorithm.HS512.getJcaName());
        secret_rt_key = new SecretKeySpec(java.util.Base64.getDecoder().decode(secretKeyRt), SignatureAlgorithm.HS512.getJcaName());
    }


    public String createAtToken(User user) {
        UUID userId = user.getId();
//        String role = user.getRole().toString();

        // claims는 페이로드 (사용자 정보)
        Claims claims = Jwts.claims().setSubject(String.valueOf(userId));
//        claims.put("role", role);

        Date now = new Date();
        String accessToken = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + accessTokenExpiryMinutes*60*1000L))
                .signWith(secret_at_key)
                .compact();

        return accessToken;
    }

    public String createRtToken(User user, boolean rememberMe) {
        UUID userId = user.getId();
//        String role = user.getUserRole().toString();

        int refreshTokenExpiryDays = rememberMe ? refreshTokenExpiryDaysPersistent : refreshTokenExpiryDaysNonPersistent;
        Claims claims = Jwts.claims().setSubject(String.valueOf(userId));
//        claims.put("role", role);
        Date now = new Date();
        String refreshToken = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + refreshTokenExpiryDays * 24 * 60 * 60 * 1000L))
                .signWith(secret_rt_key)
                .compact();

        redisTemplate.opsForValue().set("RefreshToken:"+user.getId(), refreshToken, refreshTokenExpiryDays, TimeUnit.DAYS);

        return refreshToken;
    }

    public User validateRt(String refreshToken) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secret_rt_key)
                .build()
                .parseClaimsJws(refreshToken)
                .getBody();

        String userIdString = claims.getSubject();
        UUID userId = UUID.fromString(userIdString);
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("없는 사용자"));

        String redisRt = redisTemplate.opsForValue().get("RefreshToken:"+user.getId());
        if(!redisRt.equals(refreshToken)) {
            throw new IllegalArgumentException("잘못된 토큰입니다.");
        }

        return user;
    }

    public void removeRt(String userId) {
        redisTemplate.delete("RefreshToken:"+userId);
    }
}
