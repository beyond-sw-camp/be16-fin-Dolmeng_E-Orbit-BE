package com.Dolmeng_E.drive.domain.folder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderContentsDto {
    private String name;
    private String createBy;
    private String updateAt;
    private String size;
    private String type;
}
