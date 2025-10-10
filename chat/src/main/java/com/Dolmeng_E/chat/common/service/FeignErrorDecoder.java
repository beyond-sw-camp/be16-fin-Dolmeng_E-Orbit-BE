package com.Dolmeng_E.chat.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class FeignErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        String message = "Feign 요청 중 오류가 발생했습니다.";
        int status = response.status();

        try (InputStream bodyStream = response.body().asInputStream()) {
            String body = new String(bodyStream.readAllBytes(), StandardCharsets.UTF_8);

            // JSON 파싱 시도
            Map<String, Object> map = objectMapper.readValue(body, Map.class);
            Object statusCode = map.get("statusCode");
            Object statusMessage = map.get("statusMessage");

            if (statusMessage != null) {
                message = statusMessage.toString();
            }
            if (statusCode != null) {
                status = (int) ((Number) statusCode);
            }

        } catch (IOException e) {
            System.out.println("FeignErrorDecoder JSON 파싱 실패: {}" + e.getMessage());
        }

        // 상태 코드별로 커스텀 예외 매핑
        return switch (status) {
            case 400 -> new IllegalArgumentException(message);
            case 401 -> new SecurityException(message);
            case 404 -> new IllegalStateException("요청한 리소스를 찾을 수 없습니다.");
            default -> new RuntimeException(message);
        };
    }
}
