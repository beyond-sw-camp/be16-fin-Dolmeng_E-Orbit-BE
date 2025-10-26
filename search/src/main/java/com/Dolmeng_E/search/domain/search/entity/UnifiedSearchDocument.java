package com.Dolmeng_E.search.domain.search.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Builder
@Document(indexName = "unified_search")
public class UnifiedSearchDocument {
    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String docType;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String searchTitle;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String searchContent; // 이메일, 전화번호, 상세 설명 등 추가 검색 영역
}
