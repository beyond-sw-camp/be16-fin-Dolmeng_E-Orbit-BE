package com.Dolmeng_E.drive.domain.drive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderInfoPage {
    String name;
    String parentFolderName;
    String creatorName;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    String createdBy;
}
