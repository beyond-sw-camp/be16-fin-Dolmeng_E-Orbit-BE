package com.Dolmeng_E.drive.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EditorBatchMessageDto {

    private String messageType;
    private String documentId; // 어떤 문서에 대한 메시지인지 식별
    private String senderId;   // 누가 보냈는지 식별 (임시 ID 또는 사용자 ID)
    private List<Changes> changesList = new ArrayList<>();
    private String content;
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Changes {
        // 모든 타입이 공통으로 가짐
        private String type;
        private String lineId;
        private String content;
        private String prevLineId;
    }
}
