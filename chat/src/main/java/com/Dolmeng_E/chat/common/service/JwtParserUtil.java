package com.Dolmeng_E.chat.common.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Slf4j
@Component
public class JwtParserUtil {

    private final ObjectMapper objectMapper = new ObjectMapper();
    public String extractEmailWithoutValidation(String token) {
        try {
            // JWT는 header.payload.signature 구조이므로 가운데 부분(payload)만 추출
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid JWT format");
            }

            // payload 부분 디코딩
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));

            // JSON 파싱 (ObjectMapper 사용)
            JsonNode payloadNode = objectMapper.readTree(payloadJson);
            return payloadNode.get("sub").asText();
        } catch (Exception e) {
            log.error("JWT email 추출 실패: {}", e.getMessage());
            return null;
        }
    }
}
