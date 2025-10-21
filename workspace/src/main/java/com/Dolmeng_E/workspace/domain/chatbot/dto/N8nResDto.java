package com.Dolmeng_E.workspace.domain.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class N8nResDto {
    private String text;
    private Boolean isSave;
    private String calendarName;
    private String startedAt;
    private String endedAt;
    private String calendarType;
    private Boolean bookmark;
    private Boolean isShared;
}
