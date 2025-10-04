package com.Dolmeng_E.drive.domain.folder.dto;

import com.Dolmeng_E.drive.domain.folder.entity.Folder;
import com.Dolmeng_E.drive.domain.folder.entity.RootType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderSaveDto {
    private String name;
    private String workspaceId;
    private String rootId;
    private RootType rootType;
    private String parentId;
    private String createdBy;

    public Folder toEntity(){
        return Folder.builder()
                .name(name)
                .workspaceId(workspaceId)
                .rootId(rootId)
                .rootType(rootType)
                .parentId(parentId)
                .createdBy(createdBy)
                .build();
    }
}
