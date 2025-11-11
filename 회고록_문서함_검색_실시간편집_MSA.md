## 📝 문서함, 검색, 실시간 편집 개발 회고록

### 🎯 프로젝트 개요
Orbit 프로젝트에서 문서함(Drive), 통합 검색, 실시간 문서 편집을 담당했습니다. 각 기능의 기술적 도전과 해결 과정을 정리합니다.

---

### 🏗️ MSA 아키텍처 도입 회고

#### 모놀리식에서 마이크로서비스로의 전환
처음에는 모든 기능을 하나의 애플리케이션에 구현하려 했지만, 팀 규모와 기능의 복잡성이 증가하면서 MSA로 전환하기로 결정했습니다.

**서비스 분리 전략:**
- `user-service`: 사용자 인증/인가, 프로필 관리
- `workspace-service`: 워크스페이스/프로젝트/스톤 관리
- `drive-service`: 문서함 및 파일 관리
- `search-service`: 통합 검색 및 인덱싱
- `chat-service`: 채팅 및 실시간 알림
- `api-gateway`: 라우팅 및 인증 처리
- `eureka`: 서비스 디스커버리

**배운 점:**
서비스를 어떻게 나눌지가 가장 어려웠습니다. 도메인 경계를 명확히 하는 것이 핵심이었습니다.

#### Eureka 서비스 디스커버리의 첫 만남
서비스 간 통신을 위해 Eureka를 도입했습니다. 처음에는 "이게 정말 필요한가?"라는 의문이 있었지만, 서비스가 늘어날수록 그 필요성을 느꼈습니다.

**문제점:**
- 서비스 시작 순서 의존성 (Eureka가 먼저 떠야 함)
- 네트워크 지연으로 인한 등록 지연
- 로컬 개발 환경에서의 포트 관리 복잡성

**해결:**
```yaml
# eureka 설정 - 간단해 보이지만 많은 시행착오가 있었습니다
eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
```

**아하! 순간:**
Eureka Dashboard에서 모든 서비스가 등록된 것을 보았을 때의 뿌듯함은 잊을 수 없습니다. "이제 진짜 MSA구나!"라는 생각이 들었습니다.

#### OpenFeign을 통한 서비스 간 통신
서비스 간 통신을 위해 OpenFeign을 선택했습니다. REST API 호출을 인터페이스로 추상화하는 것이 매력적이었습니다.

**초기 시도:**
```java
// 처음엔 이렇게 단순하게 생각했습니다
@FeignClient(name = "workspace-service")
public interface WorkspaceServiceClient {
    @GetMapping("/workspace/{workspaceId}/manager/check")
    ResponseEntity<CommonSuccessDto> checkWorkspaceManager(...);
}
```

**실제로 겪은 문제들:**

1. **타임아웃 설정**
   - 기본 타임아웃이 너무 짧아서 실패하는 경우가 많았습니다
   - 각 서비스의 응답 시간을 고려한 설정이 필요했습니다

2. **에러 핸들링**
   - Feign 클라이언트의 예외 처리가 까다로웠습니다
   - `FeignException`을 적절히 처리하는 로직이 필요했습니다

3. **헤더 전달**
   - `X-User-Id`, `X-Workspace-Id` 같은 헤더를 모든 요청에 포함해야 했습니다
   - `RequestInterceptor`를 사용해 해결했습니다

**개선 과정:**
```java
// 헤더 자동 전달을 위한 인터셉터
@Component
public class FeignRequestInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        // SecurityContext에서 사용자 정보 가져와서 헤더 추가
    }
}
```

#### Circuit Breaker 패턴의 필요성
서비스 간 통신에서 가장 큰 위험은 연쇄 장애(Cascading Failure)였습니다. 한 서비스가 느려지면 다른 서비스들도 영향을 받았습니다.

**Resilience4j 도입:**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      productServiceCircuit:
        failureRateThreshold: 40  # 40% 이상 실패 시 OPEN
        slowCallRateThreshold: 40  # 40% 이상 느리면 OPEN
        slowCallDurationThreshold: 2s
        waitDurationInOpenState: 10s
        slidingWindowSize: 5
```

**실제 경험:**
- `workspace-service`가 느려질 때 `drive-service`의 문서 생성이 실패하는 문제 발생
- Circuit Breaker를 적용하니 장애가 격리되어 전체 시스템이 안정적으로 동작

**아쉬운 점:**
- Fallback 메서드를 제대로 구현하지 못한 경우가 많았습니다
- 사용자에게 더 나은 에러 메시지를 제공할 수 있었을 텐데...

#### 서비스 간 데이터 동기화의 복잡성
문서함에서 문서를 생성하면 검색 서비스에도 인덱싱되어야 했습니다. 동기식 호출은 느리고, 비동기식은 일관성 문제가 있었습니다.

**해결: Kafka 이벤트 기반 아키텍처**
```java
// drive-service에서 문서 생성 시
kafkaTemplate.send("document-topic", message);

// search-service에서 이벤트 수신
@KafkaListener(topics = "document-topic")
public void consumeDocumentEvent(String message) {
    // Elasticsearch 인덱싱
}
```

**배운 점:**
- 이벤트 기반 아키텍처는 느슨한 결합을 가능하게 합니다
- 하지만 이벤트 순서 보장과 중복 처리 문제는 여전히 과제입니다

#### API Gateway의 역할
모든 요청이 API Gateway를 거치도록 설계했습니다. 인증/인가, 라우팅, 로드 밸런싱을 중앙에서 관리할 수 있었습니다.

**장점:**
- 클라이언트는 하나의 엔드포인트만 알면 됨
- 서비스별 인증 로직 중복 제거
- 요청 로깅 및 모니터링 중앙화

**어려웠던 점:**
- 라우팅 규칙 관리의 복잡성
- CORS 설정이 예상보다 까다로웠습니다

#### 모듈 공통화의 필요성
여러 서비스에서 공통으로 사용하는 DTO, 예외 처리, 유틸리티를 `module-common`으로 분리했습니다.

**장점:**
- 코드 중복 제거
- 일관된 응답 형식 (`CommonSuccessDto`)

**어려웠던 점:**
- GitHub Package Registry 설정
- 버전 관리와 의존성 업데이트의 복잡성

---

### 📁 문서함(Drive) 개발 회고

#### 초기 설계의 어려움
계층적 폴더 구조를 설계할 때 가장 고민했던 부분은 데이터 모델링이었습니다. `parentId`를 사용한 단순한 구조로 시작했지만, 재귀 쿼리와 성능 이슈가 있었습니다.

**해결 과정:**
- MariaDB의 `WITH RECURSIVE`로 조상 경로 조회 구현
- JPA의 `@OneToMany`와 `CascadeType.ALL`로 연관 관계 관리
- Soft Delete(`isDelete` 플래그)로 데이터 복구 가능성 확보

**배운 점:**
```java
// 폴더 조상 경로 조회 - 처음엔 어려웠지만 재귀 쿼리의 강력함을 느꼈습니다
@Query(value = """
    WITH RECURSIVE Ancestors AS (...)
    """, nativeQuery = true)
List<FolderInfoDto> findAncestors(@Param("id") String id);
```

#### MSA 환경에서의 권한 체크
문서함에서 파일 접근 권한을 확인하려면 `workspace-service`에 요청해야 했습니다. 동기식 호출은 느리고, 비동기식은 일관성 문제가 있었습니다.

**해결:**
```java
@FeignClient(name = "workspace-service")
public interface WorkspaceServiceClient {
    @GetMapping("/workspace/{rootId}/{rootType}/getViewableUserIds")
    Set<String> getViewableUserIds(@PathVariable String rootId, @PathVariable String rootType);
}
```

**배운 점:**
- Feign 클라이언트는 간단하지만, 네트워크 지연은 항상 고려해야 합니다
- Circuit Breaker로 장애 격리가 필수입니다

#### S3 연동의 예상치 못한 이슈
파일 업로드는 비교적 단순해 보였지만, 실제로는 여러 문제가 있었습니다.

**문제점:**
- 파일명 중복 처리
- 대용량 파일 업로드 타임아웃
- URL 생성 방식의 일관성 부족

**해결:**
- UUID 기반 파일명 생성으로 중복 방지
- `MultipartFile`의 `InputStream`을 직접 사용해 메모리 효율 개선
- S3 URL 생성 로직을 `S3Uploader`로 통합

**아쉬운 점:**
CloudFront CDN 연동을 계획했지만 시간상 S3 직접 URL로 마무리했습니다. 추후 개선이 필요합니다.

---

### 🔍 통합 검색 개발 회고

#### Elasticsearch 학습 곡선
Elasticsearch를 처음 사용하면서 가장 어려웠던 부분은 한글 검색이었습니다.

**초기 시도:**
- 기본 analyzer로는 한글 검색이 제대로 동작하지 않음
- Edge N-gram 설정이 복잡함

**해결 과정:**
1. Nori 플러그인 도입: 한글 형태소 분석
2. Custom Analyzer 설정:
   - `nori_edge_ngram_analyzer`: 인덱싱용 (1~20자 n-gram)
   - `nori_search_analyzer`: 검색용 (keyword 기반)
3. Multi-field 매핑으로 정확도와 부분 일치 모두 지원

```json
// nori-edge-ngram-analyser.json - 수많은 시행착오 끝에 완성
{
  "analyzer": {
    "nori_edge_ngram_analyzer": {
      "type": "custom",
      "tokenizer": "keyword",
      "filter": ["lowercase", "ngram_filter"]
    }
  }
}
```

#### MSA 환경에서의 검색 인덱싱
검색 서비스는 다른 서비스들의 데이터를 인덱싱해야 했습니다. 동기식 호출은 느리고, 비동기식은 일관성 문제가 있었습니다.

**해결: Kafka 이벤트 기반 인덱싱**
```java
// 다른 서비스에서 이벤트 발행
kafkaTemplate.send("document-topic", message);

// search-service에서 수신 및 인덱싱
@KafkaListener(topics = "document-topic")
public void consumeDocumentEvent(String message) {
    // Elasticsearch에 인덱싱
}
```

**배운 점:**
- 이벤트 기반 아키텍처는 느슨한 결합을 가능하게 합니다
- 하지만 이벤트 순서 보장과 중복 처리 문제는 여전히 과제입니다

#### 하이라이팅과 Inner Hits의 복잡함
문서 라인(`docLines`) 검색 시 하이라이팅이 가장 까다로웠습니다.

**문제:**
- Nested 필드의 하이라이팅이 기본적으로 동작하지 않음
- 여러 인덱스(stones, documents, files, tasks)에서 일관된 결과 필요

**해결:**
- `innerHits`와 `highlight` 조합으로 nested 필드 하이라이팅 구현
- `ignoreUnmapped(true)`로 필드가 없는 인덱스에서도 쿼리 실행 가능
- 헬퍼 메서드로 안전한 결과 처리

```java
// 이 부분이 가장 복잡했지만, 완성했을 때의 뿌듯함이...
.nested(n -> n
    .path("docLines")
    .innerHits(ih -> ih
        .highlight(h -> h
            .fields("docLines.content", f -> f
                .preTags(List.of("<em>"))
                .postTags(List.of("</em>"))
            )
        )
    )
    .ignoreUnmapped(true)
)
```

#### 자동완성의 미묘한 차이
자동완성은 `matchBoolPrefix`를 사용했습니다. 처음엔 `match_phrase_prefix`를 시도했지만, 한글에서는 `matchBoolPrefix`가 더 자연스러웠습니다.

**배운 점:**
- 검색과 자동완성은 다른 접근이 필요
- `searchTitle.ngram` 필드를 별도로 관리하는 것이 핵심

---

### ⚡ 실시간 문서 편집 개발 회고

#### WebSocket과 STOMP의 선택
실시간 편집을 위해 WebSocket을 선택했고, STOMP를 프로토콜로 사용했습니다.

**초기 고민:**
- 순수 WebSocket vs STOMP
- 단일 서버 vs Redis Pub/Sub

**결정:**
- STOMP: 메시징 구조화와 구독/발행 패턴 활용
- Redis Pub/Sub: 다중 서버 환경 대비

#### MSA 환경에서의 실시간 통신
여러 서버 인스턴스가 있을 때, 한 서버에서 보낸 메시지를 다른 서버의 클라이언트도 받아야 했습니다.

**해결: Redis Pub/Sub**
```java
// 메시지 발행
redisTemplate.convertAndSend("document-updates", message);

// 구독 및 브로드캐스트
@RedisListener(pattern = "document-updates")
public void onMessage(Message message) {
    messagingTemplate.convertAndSend(destination, editorMessage);
}
```

**배운 점:**
- 단일 서버에서는 WebSocket만으로도 충분하지만, MSA 환경에서는 Redis Pub/Sub이 필수입니다
- 메시지 순서 보장과 중복 처리 문제는 여전히 과제입니다

#### 라인 단위 잠금의 함정
동시 편집 충돌을 막기 위해 라인 단위 잠금을 구현했습니다.

**문제:**
- Redis `putIfAbsent`의 동시성 처리
- 사용자 연결 종료 시 잠금 해제 누락
- 데드락 가능성

**해결:**
```java
// Redis Hash로 라인별 잠금 관리
if (Boolean.TRUE.equals(hashOperations.putIfAbsent("lock:"+key, lineId, message.getSenderId()))) {
    setOperations.add("user_lock:"+key+message.getSenderId(), lineId);
    return true;
}
```

**개선:**
- `user_lock` Set으로 사용자별 잠금 추적
- `leaveUser`에서 자동 잠금 해제
- 연결 종료 시 `afterConnectionClosed`에서 정리

#### 배치 업데이트의 성능 고민
한 번에 여러 라인을 수정할 때 개별 DB 업데이트는 비효율적이었습니다.

**최적화:**
- 배치 업데이트로 트랜잭션 수 감소
- CREATE/UPDATE/DELETE를 하나의 메시지로 처리
- Kafka로 검색 인덱스 비동기 업데이트

```java
// 배치 처리로 성능 개선
public void batchUpdateDocumentLine(EditorBatchMessageDto messages) {
    for(EditorBatchMessageDto.Changes changes : messages.getChangesList()) {
        if(changes.getType().equals("UPDATE")) {
            updateDocumentLine(changes.getLineId(), changes.getContent());
        }
        // ...
    }
}
```

#### 커서 동기화의 실시간성
커서 위치는 DB에 저장하지 않고 실시간으로만 전송합니다.

**이유:**
- 커서는 일시적 정보
- DB 저장 시 불필요한 부하
- Redis Pub/Sub으로 즉시 브로드캐스트

**아쉬운 점:**
- 커서 위치 히스토리는 저장하지 않음
- 나중에 "누가 어디 편집했는지" 추적 기능 추가 고려

---

### 🎓 종합 회고

#### MSA 아키텍처의 장단점

**장점:**
- 서비스별 독립적인 배포와 스케일링
- 기술 스택 선택의 자유도
- 팀별 독립적인 개발 가능

**단점:**
- 복잡도 증가 (네트워크, 분산 트랜잭션)
- 디버깅의 어려움
- 운영 복잡도 증가

**배운 점:**
MSA는 만능이 아닙니다. 프로젝트 규모와 팀 규모를 고려해 도입해야 합니다.

#### 기술 스택 선택의 중요성
- **Elasticsearch**: 검색 기능 구현에 적합
- **Redis**: 실시간 기능과 캐싱에 유용
- **Kafka**: 이벤트 기반 아키텍처로 느슨한 결합 달성
- **Eureka**: 서비스 디스커버리로 동적 라우팅 가능
- **OpenFeign**: 선언적 서비스 간 통신
- **Resilience4j**: 장애 격리와 회복력 향상

#### 아키텍처 설계의 깊이
- 초기에는 단순하게 시작
- 점진적으로 복잡도 증가
- 리팩토링과 최적화를 반복

#### 협업의 중요성
- 프론트엔드와의 WebSocket 프로토콜 협의
- 검색 결과 DTO 설계 협업
- 서비스 간 API 계약 정의
- 문서화의 중요성

#### 개선하고 싶은 부분
1. **모니터링 강화**: 각 서비스의 메트릭 수집 및 대시보드 구축
2. **분산 추적**: Zipkin이나 Jaeger로 요청 추적
3. **API 버전 관리**: 서비스 간 호환성 유지
4. **테스트 전략**: 통합 테스트와 E2E 테스트 강화
5. **CloudFront CDN 연동**: 파일 서빙 성능 개선
6. **검색 결과 캐싱**: 응답 시간 단축
7. **편집 히스토리 저장**: 버전 관리 기능
8. **검색 쿼리 성능 모니터링**: 최적화 지점 파악

---

### 💡 마무리

문서함, 검색, 실시간 편집을 MSA 환경에서 개발하며 기술적 깊이와 아키텍처 설계의 중요성을 배웠습니다. 특히 Elasticsearch의 복잡성, WebSocket의 실시간성, Redis의 동시성 처리, 그리고 MSA의 분산 시스템 특성이 인상적이었습니다.

**가장 큰 교훈:**
"MSA는 기술이 아니라 아키텍처 철학이다. 서비스를 나누는 것보다 서비스 간의 경계를 명확히 하는 것이 더 중요하다."

앞으로는 성능 최적화와 사용자 경험 개선에 집중하고, 더 나은 협업 도구를 만들어가겠습니다.

**"기술은 도구일 뿐, 사용자의 경험이 최우선이다"** — 이번 프로젝트를 통해 다시 한번 확인했습니다.

---

### 📚 참고한 기술 문서
- Spring Cloud OpenFeign
- Netflix Eureka
- Resilience4j Circuit Breaker
- Elasticsearch Nori Analyzer
- Redis Pub/Sub
- Apache Kafka
- WebSocket STOMP Protocol


