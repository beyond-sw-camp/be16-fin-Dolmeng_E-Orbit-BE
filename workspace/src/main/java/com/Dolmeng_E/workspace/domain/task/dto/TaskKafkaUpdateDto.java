package com.Dolmeng_E.workspace.domain.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskKafkaUpdateDto {
    private String eventType;
    private EventPayload eventPayload;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EventPayload{
        private String id;
        private String name;
        private Set<String> viewableUserIds;
        private LocalDateTime endDate;
        private String status;
        private String manager;
    }
}
