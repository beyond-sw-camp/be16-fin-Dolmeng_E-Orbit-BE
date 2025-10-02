package com.Dolmeng_E.workspace.common.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommonSuccessDto {
    private Object result;
    private int statusCode;
    private String statusMessage;
}
