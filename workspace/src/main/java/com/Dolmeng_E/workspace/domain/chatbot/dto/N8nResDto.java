package com.Dolmeng_E.workspace.domain.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class N8nResDto {
    private String text;
    private String calendarName;
    private String startedAt;
    private String endedAt;
    private String calendarType;
    private Boolean bookmark;
    private Boolean isShared;
}
