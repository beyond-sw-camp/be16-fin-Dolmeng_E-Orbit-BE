package com.Dolmeng_E.search.domain.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentSearchResDto {
    private String id;
    private String docType;
    private String searchTitle;
    private String searchContent;
    private List<participantInfo> participantInfos;
    private Boolean isDone;
    private String stoneStatus;
    private String creatorName;
    private String createdBy;
    private String profileImageUrl;
//  파일, 문서는 생성일, 스톤과 테스크는 마감일
    private LocalDate dateTime;
    private String rootType;
    private String rootId;
    private String parentId;
    private String fileUrl;
    private Long size;
    private String projectId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class participantInfo{
        private String id;
        private String name;
        private String profileImageUrl;
    }
}
