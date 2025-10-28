package com.Dolmeng_E.search.domain.search.entity; // TaskDocument와 같은 패키지

import lombok.Builder;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Builder
public class ParticipantInfo {

    // 참여자 ID (식별용)
    @Field(type = FieldType.Keyword)
    private String id;

    // 참여자 이름 (검색 대상)
    @Field(type = FieldType.Text, analyzer = "nori")
    private String name;

    // 참여자 프로필 이미지 (UI 표시용)
    @Field(type = FieldType.Keyword, index = false) // index=false: 검색 안 함
    private String profileImageUrl;
}