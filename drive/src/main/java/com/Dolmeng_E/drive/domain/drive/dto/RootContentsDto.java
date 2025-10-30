package com.Dolmeng_E.drive.domain.drive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RootContentsDto {
    private String name;
    private String createBy;
    private String updateAt;
    private Long size;
    private String type;
    private String id;
    private String creatorName;
    private String profileImage;
    private List<FolderInfoDto> ancestors;
}
