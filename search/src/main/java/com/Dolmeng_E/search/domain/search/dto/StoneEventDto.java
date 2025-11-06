package com.Dolmeng_E.search.domain.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoneEventDto {
    private String eventType;
    private EventPayload eventPayload;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EventPayload{
        private String id;
        private String name;
        private String description;
        private Set<String> viewableUserIds;
        private LocalDateTime endDate;
        private String projectId;
        private String rootType;
        private String status;
        private List<ParticipantInfo> participants;
        private String manager;
        private String workspaceId;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class ParticipantInfo{
            private String id;
        }
    }
}
