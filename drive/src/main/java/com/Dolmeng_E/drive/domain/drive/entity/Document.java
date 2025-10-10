package com.Dolmeng_E.drive.domain.drive.entity;

import com.example.modulecommon.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "document_seq")
    @GenericGenerator(
            name = "document_seq", // generator 이름
            strategy = "com.Dolmeng_E.drive.common.StringPrefixedSequenceIdGenerator", // 1단계에서 만든 클래스 경로
            parameters = {
                    @Parameter(name = "sequence_name", value = "document_seq"), // DB에 생성할 시퀀스 이름
                    @Parameter(name = "initial_value", value = "1"), // 시퀀스 시작 값
                    @Parameter(name = "increment_size", value = "1"), // 시퀀스 증가 값
                    @Parameter(name = "valuePrefix", value = "ws_fol_doc_") // ID에 붙일 접두사!
            }
    )
    private String id;

    @Column(nullable = false)
    @Builder.Default
    private String title = "제목없음";

    @Lob // 내용이 매우 길 수 있음을 나타냄
    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private String createdBy;

    private String updatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;
}
