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
public class File extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "file_seq")
    @GenericGenerator(
            name = "file_seq", // generator 이름
            strategy = "com.Dolmeng_E.drive.common.StringPrefixedSequenceIdGenerator", // 1단계에서 만든 클래스 경로
            parameters = {
                    @Parameter(name = "sequence_name", value = "file_seq"), // DB에 생성할 시퀀스 이름
                    @Parameter(name = "initial_value", value = "1"), // 시퀀스 시작 값
                    @Parameter(name = "increment_size", value = "1"), // 시퀀스 증가 값
                    @Parameter(name = "valuePrefix", value = "ws_fol_f_") // ID에 붙일 접두사!
            }
    )
    private String id;

    @Column(nullable = false)
    private String workspaceId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private Long size;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDelete = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    @Column(nullable = false)
    private String rootId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RootType rootType;

    public void updateIsDelete() {this.isDelete = true;}
    public void updateFolder(Folder folder) {this.folder = folder;}
    public void updateName(String name) {this.name = name;}
}
