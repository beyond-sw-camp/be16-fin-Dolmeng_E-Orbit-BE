package com.Dolmeng_E.search.domain.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentSuggestResDto {
    private String id;
    private String docType;
    private String searchTitle;
    private String rootType;
    private String rootId;
}
