package com.Dolmeng_E.search.domain.search.entity;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentLine {
    @Field(type = FieldType.Keyword, index = false)
    private Long id;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String content;
}