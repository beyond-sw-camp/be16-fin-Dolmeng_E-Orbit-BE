package com.Dolmeng_E.drive.domain.drive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentKafkaDto {
    private String eventType;
    private EventPayload eventPayload;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EventPayload{
        private String originalId;
        private String searchTitle;
        private String searchContent;
    }
}
