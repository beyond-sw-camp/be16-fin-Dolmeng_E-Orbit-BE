package com.Dolmeng_E.drive.domain.drive.entity;

import com.example.modulecommon.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Folder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "folder_seq")
    @GenericGenerator(
            name = "folder_seq", // generator 이름
            strategy = "com.Dolmeng_E.drive.common.StringPrefixedSequenceIdGenerator", // 1단계에서 만든 클래스 경로
            parameters = {
                    @Parameter(name = "sequence_name", value = "folder_seq"), // DB에 생성할 시퀀스 이름
                    @Parameter(name = "initial_value", value = "1"), // 시퀀스 시작 값
                    @Parameter(name = "increment_size", value = "1"), // 시퀀스 증가 값
                    @Parameter(name = "valuePrefix", value = "ws_fol_") // ID에 붙일 접두사!
            }
    )
    private String id;

    @Column(nullable = false)
    private String workspaceId;

    @Column(nullable = false)
    private String rootId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RootType rootType;

    @Column(nullable = false)
    private String name;

    private String parentId;

    @Column(nullable = false)
    private String createdBy;

    private String updatedBy;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDelete = false;

    @Builder.Default
    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<File> files = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Document> documents = new ArrayList<>();

    public void updateName(String updatedName) {
        this.name = updatedName;
    }
    public void updateIsDelete() {this.isDelete = true;}
    public void updateParentId(String ParentId) {this.parentId = ParentId;}
}
