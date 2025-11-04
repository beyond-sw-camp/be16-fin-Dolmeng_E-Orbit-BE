package com.Dolmeng_E.drive.domain.drive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentKafkaSaveDto {
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
        private String createdBy;
        private List<String> viewableUserIds;
        private LocalDateTime createdAt;
        private String rootType;
        private String rootId;
        private String parentId;
        private String fileUrl;
        private Long size;
    }
}
