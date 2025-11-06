package com.Dolmeng_E.search.domain.search.dto;

import com.Dolmeng_E.search.domain.search.entity.DocumentLine;
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
public class EventDto {
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
        private Set<String> viewableUserIds;
        private LocalDateTime createdAt;
        private String rootType;
        private String rootId;
        private String parentId;
        private String fileUrl;
        private Long size;
        private String workspaceId;
        List<DocumentLineDto> docLines;
    }
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DocumentLineDto{
        private Long id;
        private String content;
        private String type;
    }
}

