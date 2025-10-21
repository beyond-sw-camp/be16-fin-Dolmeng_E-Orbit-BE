package com.Dolmeng_E.workspace.domain.chatbot.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

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

        Map<byte[], byte[]> map = new HashMap<>();
        map.put("role".getBytes(StandardCharsets.UTF_8), role.getBytes(StandardCharsets.UTF_8));
        map.put("text".getBytes(StandardCharsets.UTF_8), text.getBytes(StandardCharsets.UTF_8));
        map.put("ts".getBytes(StandardCharsets.UTF_8), String.valueOf(Instant.now().toEpochMilli()).getBytes(StandardCharsets.UTF_8));
        map.put("embedding".getBytes(StandardCharsets.UTF_8), binary);

        jedis.hset(key.getBytes(StandardCharsets.UTF_8), map);
        jedis.expire(key, 1800); // TTL 30분

        log.info("SemanticMemoryService - saveToRedis() - Saved -> " + key);
    }


    // --- 3. workspace, user로 필터링 후 유사도가 가장 좋은 질문의 답 반환 ---
    public String findBotReplyByKeyFilter(String workspaceId, String userId, String query) {
        float[] queryEmbedding = createEmbedding(query);

        // 1. 유저 prefix 키 범위
        String prefix = String.format("chatmem:USER:%s:%s:", workspaceId, userId);

        // 2. prefix로 키 스캔
        List<String> keys = new ArrayList<>();
        String cursor = "0";
        do {
            ScanResult<String> res = jedis.scan(cursor, new ScanParams().match(prefix + "*").count(100));
            cursor = res.getCursor();
            keys.addAll(res.getResult());
        } while (!cursor.equals("0"));

        if (keys.isEmpty()) return null;

        // 3. 각 키별 임베딩 불러와 유사도 계산
        String bestKey = null;
        double bestScore = Double.MAX_VALUE; // 코사인 거리: 작을수록 유사
        for (String key : keys) {
            byte[] embBytes = jedis.hget(key.getBytes(StandardCharsets.UTF_8), "embedding".getBytes(StandardCharsets.UTF_8));
            if (embBytes == null) continue;

            float[] emb = bytesToFloatArray(embBytes);
            double score = cosineDistance(queryEmbedding, emb);
            if (score < bestScore) {
                bestScore = score;
                bestKey = key;
            }
        }

        if (bestKey == null) return null;

        // 4. 같은 UUID로 BOT 키 생성
        String uuid = bestKey.substring(bestKey.lastIndexOf(":") + 1);
        String botKey = bestKey.replace("USER", "BOT").replace(uuid, uuid);

        // 5. BOT 텍스트 가져오기
        String botReply = jedis.hget(botKey, "text");
        log.info("SemanticMemoryService - findBotReplyByKeyFilter() - bestScore: " + bestScore + ", Bot reply: " + botReply);

        // 6. 유사도가 0.25 이하일 때만 답장 반환
        if(bestScore <= 0.25) {
            return botReply;
        } else {
            return null;
        }
    }

    private static double cosineDistance(float[] a, float[] b) {
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return 1.0 - (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }

    private static float[] bytesToFloatArray(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        float[] arr = new float[bytes.length / 4];
        for (int i = 0; i < arr.length; i++) arr[i] = bb.getFloat();
        return arr;
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

