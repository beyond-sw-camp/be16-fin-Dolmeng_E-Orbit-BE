package com.Dolmeng_E.search.domain.search.entity;

import com.Dolmeng_E.search.domain.search.dto.EventDto;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@Document(indexName = "files")
@Setting(settingPath = "elasticsearch/nori-edge-ngram-analyser.json")
public class FileDocument {
    // --- 공통 식별 필드 ---
    // 검색 대상 ID
    @Id
    private String id;

    // 생성자
    @Field(type = FieldType.Keyword, index = false)
    private String createdBy;

    // 검색할 대상 타입
    @Builder.Default
    @Field(type = FieldType.Keyword)
    private String docType = "FILE";

    // --- 검색 대상 필드 ---
    // 제목
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "nori"), // searchTitle
            otherFields = {
                    @InnerField( // searchTitle.ngram
                            suffix = "ngram",
                            type = FieldType.Text,
                            analyzer = "nori_edge_ngram_analyzer",
                            searchAnalyzer = "nori_search_analyzer"
                    )
            }
    )
    private String searchTitle;

    // 파일 내용
    @Field(type = FieldType.Text, analyzer = "nori")
    private String searchContent;

    // --- 권한/정렬 필드 ---
    // 해당 폴더 권한 + 관리자
    @Field(type = FieldType.Keyword)
    private Set<String> viewableUserIds;

    // --- URL 생성 및 UI 표시용 필드 ---
    // 생성일
    @Field(type = FieldType.Date)
    private LocalDate dateTime;

    // 생성자 프로필 이미지
    @Field(type = FieldType.Keyword, index = false)
    private String profileImageUrl;

    // 생성자 이름
    @Field(type = FieldType.Keyword, index = false)
    private String creatorName;

    // 루트 경로
    @Field(type = FieldType.Keyword, index = false)
    private String rootType;

    // 루트 ID
    @Field(type = FieldType.Keyword, index = false)
    private String rootId;

    // 상위 폴더
    @Field(type = FieldType.Keyword, index = false)
    private String parentId;

    @Field(type = FieldType.Keyword, index = false)
    private String fileUrl;

    @Field(type = FieldType.Keyword, index = false)
    private Long size;

    @Field(type = FieldType.Keyword, index = false)
    private String workspaceId;

    public void updateFile(EventDto.EventPayload eventPayload){
        this.searchTitle = eventPayload.getSearchTitle();
    }
}
