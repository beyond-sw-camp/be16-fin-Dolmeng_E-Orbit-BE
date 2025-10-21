package com.Dolmeng_E.user.domain.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchDto {
    String searchKeyword;
}
