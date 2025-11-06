package com.Dolmeng_E.drive.domain.drive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderResDto {
    String rootType;
    String rootId;
    String rootName;
    String folderId;
    String folderName;
}
