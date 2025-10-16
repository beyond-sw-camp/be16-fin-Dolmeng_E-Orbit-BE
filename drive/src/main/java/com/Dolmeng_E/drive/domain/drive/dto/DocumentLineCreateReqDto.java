package com.Dolmeng_E.drive.domain.drive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentLineCreateReqDto {
    private String documentId;
    private String lineId;
    private String content;
    private String prevLineId;
}
