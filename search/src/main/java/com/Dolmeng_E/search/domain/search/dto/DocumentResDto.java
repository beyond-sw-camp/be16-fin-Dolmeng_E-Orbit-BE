package com.Dolmeng_E.search.domain.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResDto {
    private String originalId;
    private String searchTitle;
    private String searchContent;
    private String projectId;
    private String stoneId;
    private Boolean isCompleted;
    private String profileImageUrl;
}
