package com.Dolmeng_E.api_gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthFilter implements GlobalFilter {

    @Value("${jwt.secretKeyAt}")
    private String secretKeyAt;

    private static final List<String> ALLOWED_PATH = List.of(
            "/user",
            "/user/auth/login",
            "/user/kakao/login",
            "/user/google/login",
            "/auth/email",
            "/user/email",
            "/user/authcode",
            "/user/auth/token",
            "/user/password/email",
            "/user/password/authcode",
            "/user/password",
            "/health",
            "/connect/**",
            "/chatbot/project-info",
            "/chatbot/task-list",
            "/chatbot/unread-messages",
            "/chatbot/history"
    );

    private static final List<String> ADMIN_ONLY_PATH = List.of(
            "/user/list"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String bearerToken = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String urlPath = exchange.getRequest().getURI().getPath();

        // 인증이 필요 없는 경로는 필터 통과 (하위 경로 포함)
        if(ALLOWED_PATH.stream().anyMatch(path -> urlPath.startsWith(path.replace("/**", "")))) {
            return chain.filter(exchange);
        }


        try{
            if(bearerToken == null || !bearerToken.startsWith("Bearer ")) {
                throw  new IllegalArgumentException("token이 없거나, 형식이 잘못되었습니다.");
            }
            String token = bearerToken.substring(7);

//            token 검증 및 payload 추출
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKeyAt)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String userIdString = claims.getSubject();
            String role = claims.get("role", String.class);
            
//            admin권한 있어야 하는 url 검증
            if(ADMIN_ONLY_PATH.contains(urlPath) && !role.equals(("ADMIN"))){
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

//            header에 email, role 등 payload값 세팅
//            X를 붙이는 것은 custom header라는 것을 의미하는 관례적 키워드
//            주로 서비스 모듈에서 RequestHeader어노테이션을 사용하여 아래 헤더를 꺼내 쓸 수 있음
            ServerWebExchange serverWebExchange = exchange.mutate()
                    .request(r -> r.header("X-User-Id", userIdString)
                            .header("X-User-Role", role))
                    .build();

            return chain.filter(serverWebExchange);
        }catch (Exception e){
            e.printStackTrace();
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//            추가적인 메시지는 필요시 세팅
            return exchange.getResponse().setComplete();
        }
    }
}
