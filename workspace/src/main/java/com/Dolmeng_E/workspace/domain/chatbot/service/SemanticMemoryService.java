package com.Dolmeng_E.workspace.domain.chatbot.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import redis.clients.jedis.Jedis;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Component
@Slf4j
public class SemanticMemoryService {

    private final WebClient webClient;
    private final String apiKey;
    private final Jedis jedis;

    public SemanticMemoryService(
            @Value("${openai.api.key}") String apiKey,
            @Value("${spring.redis-stack.host}") String redisHost,
            @Value("${spring.redis-stack.port}") int redisPort
    ) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.jedis = new Jedis(redisHost, redisPort);
    }

    // --- 1. OpenAI 임베딩 생성 ---
    public float[] createEmbedding(String text) {
        EmbReq req = new EmbReq("text-embedding-3-small", text);

        EmbRes res = webClient.post()
                .uri("/embeddings")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(EmbRes.class)
                .block();

        List<Double> list = res.getData().get(0).getEmbedding();
        float[] vec = new float[list.size()];
        for (int i = 0; i < list.size(); i++) vec[i] = list.get(i).floatValue();
        return vec;
    }

    // --- 2. Redis 저장 ---
    public void saveToRedis(String userId, String workspaceId, String role, String text, UUID uuidKey) {
        float[] embedding = createEmbedding(text);
        byte[] binary = floatArrayToBytes(embedding);

        // 키 네임스페이스 구조: chatmem:USER:{workspaceId}:{userId}:{UUID}
        String key = String.format("chatmem:%s:%s:%s:%s", role, workspaceId, userId, uuidKey);

        // TAG 값 정규화: '-', '_', 공백 등 제거
        String safeUserId = normalizeTag(userId);
        String safeWorkspaceId = normalizeTag(workspaceId);
        String safeRole = normalizeTag(role);

        Map<byte[], byte[]> map = new HashMap<>();
        map.put("role".getBytes(StandardCharsets.UTF_8), safeRole.getBytes(StandardCharsets.UTF_8));
        map.put("workspaceId".getBytes(StandardCharsets.UTF_8), safeWorkspaceId.getBytes(StandardCharsets.UTF_8));
        map.put("userId".getBytes(StandardCharsets.UTF_8), safeUserId.getBytes(StandardCharsets.UTF_8));
        map.put("text".getBytes(StandardCharsets.UTF_8), text.getBytes(StandardCharsets.UTF_8));
        map.put("ts".getBytes(StandardCharsets.UTF_8),
                String.valueOf(Instant.now().toEpochMilli()).getBytes(StandardCharsets.UTF_8));
        map.put("embedding".getBytes(StandardCharsets.UTF_8), binary);

        jedis.hset(key.getBytes(StandardCharsets.UTF_8), map);
        jedis.expire(key, 1800); // TTL 30분

        log.info("SemanticMemoryService - saveToRedis() - Saved -> " + key);
    }

    // --- 3. workspace, user로 필터링 후 유사도가 가장 좋은 질문의 답 반환 ---
    public String findBotReplyWithKnn(String workspaceId, String userId, String query) {
        // 1) 쿼리 벡터
        float[] emb = createEmbedding(query);
        byte[] vec = floatArrayToBytes(emb);

        // 2) TAG 정규화 (저장 시에도 동일 규칙으로 넣었어야 함)
        String ws = normalizeTag(workspaceId);
        String uid = normalizeTag(userId);

        // 3) 하이브리드 KNN 쿼리
        String q = String.format(
                "(@role:{USER} @workspaceId:{%s} @userId:{%s})=>[KNN 1 @embedding $vec AS score]",
                ws, uid
        );

        Object result = jedis.sendCommand(
                (redis.clients.jedis.commands.ProtocolCommand) () -> "FT.SEARCH".getBytes(),
                "chatmem_idx".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                q.getBytes(java.nio.charset.StandardCharsets.UTF_8),

                // PARAMS (name, value 쌍의 갯수)
                "PARAMS".getBytes(), "2".getBytes(),
                "vec".getBytes(), vec,

                // 정렬/반환/다이얼렉트/리밋
                "SORTBY".getBytes(), "score".getBytes(),
                "RETURN".getBytes(), "3".getBytes(),
                "text".getBytes(), "score".getBytes(), "ts".getBytes(),
                "DIALECT".getBytes(), "2".getBytes(),
                "LIMIT".getBytes(), "0".getBytes(), "1".getBytes()
        );

        if (!(result instanceof java.util.List<?> list) || list.size() < 3) return null;

        String matchedKey = new String((byte[]) list.get(1), java.nio.charset.StandardCharsets.UTF_8);
        java.util.List<?> fields = (java.util.List<?>) list.get(2);

        String botReplyKey = matchedKey.replace("chatmem:USER:", "chatmem:BOT:");
        String scoreStr = null;
        for (int i = 0; i < fields.size(); i += 2) {
            String f = new String((byte[]) fields.get(i), java.nio.charset.StandardCharsets.UTF_8);
            String v = new String((byte[]) fields.get(i + 1), java.nio.charset.StandardCharsets.UTF_8);
            if ("score".equals(f)) scoreStr = v;
        }
        double score = (scoreStr != null) ? Double.parseDouble(scoreStr) : Double.POSITIVE_INFINITY;

        if (score <= 0.25) {
            log.info("SemanticMemoryService - findBotReplyWithKnn() - 유사도 0.25 통과 score -> " + score);
            return jedis.hget(botReplyKey, "text");
        }
        return null;
    }

    private String normalizeTag(String v) {
        return v == null ? "" : v.replaceAll("[-_\\s]", "");
    }

    // --- 유틸 ---
    private static byte[] floatArrayToBytes(float[] arr) {
        ByteBuffer bb = ByteBuffer.allocate(arr.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float v : arr) bb.putFloat(v);
        return bb.array();
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // --- 내부 DTO ---
    public record EmbReq(String model, String input) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EmbData {
        private List<Double> embedding;
        public List<Double> getEmbedding() { return embedding; }
        public void setEmbedding(List<Double> embedding) { this.embedding = embedding; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EmbRes {
        private List<EmbData> data;
        public List<EmbData> getData() { return data; }
        public void setData(List<EmbData> data) { this.data = data; }
    }
}

