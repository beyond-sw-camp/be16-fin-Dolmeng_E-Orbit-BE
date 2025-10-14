package com.Dolmeng_E.chat.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class ChatFileListReqDto {
    @Builder.Default
    private List<MultipartFile> fileList = new ArrayList<>();
}
