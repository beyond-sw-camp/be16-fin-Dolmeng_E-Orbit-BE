package com.Dolmeng_E.drive.domain.drive.dto;

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
public class DocumentKafkaUpdateDto {
    private String eventType;
    private EventPayload eventPayload;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EventPayload{
        private String id;
        private String searchTitle;
        private String searchContent;
        private Set<String> viewableUserIds;
        private String parentId;
        private String rootType;
        private String rootId;
        private List<DocumentKafkaSaveDto.DocumentLineDto> docLines;
    }
}
