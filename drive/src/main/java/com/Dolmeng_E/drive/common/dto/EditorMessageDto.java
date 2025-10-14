package com.Dolmeng_E.drive.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EditorMessageDto {
    public enum MessageType {
        JOIN, // 특정 문서 편집 세션에 참여
        UPDATE, // 문서 내용 업데이트
        CURSOR_UPDATE // 커서 위치 업데이트
    }

    private MessageType type;
    private String documentId; // 어떤 문서에 대한 메시지인지 식별
    private String senderId;   // 누가 보냈는지 식별 (임시 ID 또는 사용자 ID)
    private Object content;    // 실제 문서 변경 내용 (JSON 형태의 Delta)
}
