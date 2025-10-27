package com.Dolmeng_E.search.domain.search.dto;

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
public class DocumentResDto {
    private String Id;
    private String searchTitle;
    private String searchContent;
    private List<participantInfo> participants;
    private Boolean isDone;
    private String stoneStatus;
    private String creatorName;
    private String createdBy;
    private String profileImageUrl;
//  파일, 문서는 생성일, 스톤과 테스크는 마감일
    private LocalDateTime dateTime;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class participantInfo{
        private String id;
        private String name;
        private String profileImage;
    }
}
