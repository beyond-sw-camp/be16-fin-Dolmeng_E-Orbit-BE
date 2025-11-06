package com.Dolmeng_E.workspace.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class RestTemplateClient {
    private final RestTemplate restTemplate;

    /**
     * 단순 GET 요청
     */
    public <T> ResponseEntity<T> get(String url, Class<T> responseType) {
        return restTemplate.getForEntity(url, responseType);
    }

    /**
     * 단순 POST 요청 (body 포함)
     */
    public <T> ResponseEntity<T> post(String url, Object body, Class<T> responseType) {
        return restTemplate.postForEntity(url, body, responseType);
    }

    /**
     * 헤더 포함 POST 요청 (ex: Authorization)
     */
    public <T> ResponseEntity<T> postWithHeader(String url, Object body, Class<T> responseType, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);

        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
    }

    /**
     * PUT, DELETE 등 확장용 (exchange 직접 사용)
     */
    public <T> ResponseEntity<T> exchange(String url, HttpMethod method, Object body, Class<T> responseType, HttpHeaders headers) {
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(url, method, entity, responseType);
    }
}
