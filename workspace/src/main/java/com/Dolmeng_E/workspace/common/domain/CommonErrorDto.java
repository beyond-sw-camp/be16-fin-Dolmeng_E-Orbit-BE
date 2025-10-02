package com.Dolmeng_E.workspace.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonErrorDto {
    private int statusCode;
    private String statusMessage;
}
