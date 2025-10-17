package com.Dolmeng_E.drive.domain.drive.entity;

import com.example.modulecommon.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentLine extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", length = 4000)
    private String content;

    private String prevId;

    @Column(unique = true)
    private String lineId; // 프론트에서 적용해주는 uuid 형식의 id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    public void updateContent(String content) {
        this.content = content;
    }
    public void updatePrevId(String prevId){
        this.prevId = prevId;
    }
}
