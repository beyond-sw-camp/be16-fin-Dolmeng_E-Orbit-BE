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
public class FileInfoPage {
    String name;
    Long fileSize;
    String folderName;
    String creatorName;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    String createdBy;
}
