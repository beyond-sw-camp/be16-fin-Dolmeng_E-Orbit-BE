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
import java.util.concurrent.TimeUnit;

@Component
public class JwtTokenProvider {
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.expirationAt}")
    private int expirationAt;

    @Value("${jwt.secretKeyAt}")
    private String secretKeyAt;

    @Value("${jwt.expirationRt}")
    private int expirationRt;

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
        String email = user.getEmail();
//        String role = user.getRole().toString();

        // claims는 페이로드 (사용자 정보)
        Claims claims = Jwts.claims().setSubject(email);
//        claims.put("role", role);

        Date now = new Date();
        String accessToken = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationAt*60*1000L)) // 30분을 세팅
                // secret 키를 통해 signiture 생성
                .signWith(secret_at_key)
                .compact();

        return accessToken;
    }

    public String createRtToken(User user) {
        String email = user.getEmail();
//        String role = member.getRole().toString();

        // claims는 페이로드 (사용자 정보)
        Claims claims = Jwts.claims().setSubject(email);
//        claims.put("role", role);

        Date now = new Date();
        String refreshToken = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationRt*60*1000L))
                .signWith(secret_rt_key)
                .compact();

        redisTemplate.opsForValue().set("RefreshToken:"+user.getEmail(), refreshToken, 200, TimeUnit.DAYS); // 200일 ttl


        return refreshToken;
    }

    public User validateRt(String refreshToken) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secret_rt_key)
                .build()
                .parseClaimsJws(refreshToken)
                .getBody();

        String email = claims.getSubject();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("없는 사용자"));

        String redisRt = redisTemplate.opsForValue().get("RefreshToken:"+user.getEmail());
        if(!redisRt.equals(refreshToken)) {
            throw new IllegalArgumentException("잘못된 토큰입니다.");
        }

        return user;
    }
}
