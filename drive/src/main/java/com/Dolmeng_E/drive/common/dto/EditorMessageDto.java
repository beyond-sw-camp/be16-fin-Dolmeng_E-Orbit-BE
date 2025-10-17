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
        CREATE, // 새 라인 생성
        UPDATE, // 라인 수정
        DELETE, // 라인 삭제
        CURSOR_UPDATE, // 커서 위치 업데이트
        JOIN // 문서 참여
    }

    private MessageType messageType;

    private String documentId; // 어떤 문서에 대한 메시지인지 식별
    private String senderId;   // 누가 보냈는지 식별 (임시 ID 또는 사용자 ID)
    private String lineId;
    private String prevLineId;
    private String content;
}
