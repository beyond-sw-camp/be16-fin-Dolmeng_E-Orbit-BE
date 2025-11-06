package com.Dolmeng_E.search.domain.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoneViewableUpdateEventDto {
    private String eventType;
    private EventPayload eventPayload;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EventPayload{
        private String id;
        // 추가인지 삭제 인지
        private String type;
        // 스톤인지 워크스페이스인지
        private String rootType;
        private String projectId;
        // ID
        private Set<UUID> userIds;
    }
}
