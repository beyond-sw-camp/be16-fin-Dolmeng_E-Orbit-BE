package com.Dolmeng_E.chat_db.domain.dto;

import com.Dolmeng_E.chat_db.domain.entity.ChatFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class ChatFileListDto {
    private Long fileId;
    private String fileName;
    private Long fileSize;
    private String fileUrl;
    private LocalDateTime createAt;

    static public ChatFileListDto fromEntity(ChatFile chatFile) {
        return ChatFileListDto.builder()
                .fileId(chatFile.getId())
                .fileName(chatFile.getFileName())
                .fileSize(chatFile.getFileSize())
                .fileUrl(chatFile.getFileUrl())
                .createAt(chatFile.getCreatedAt())
                .build();
    }
}
