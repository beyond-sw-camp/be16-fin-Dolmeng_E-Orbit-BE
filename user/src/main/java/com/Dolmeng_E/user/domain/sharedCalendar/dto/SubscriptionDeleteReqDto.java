package com.Dolmeng_E.user.domain.sharedCalendar.dto;

import lombok.Getter;
import java.util.List;

@Getter
public class SubscriptionDeleteReqDto {
    private List<String> subscriptionIdList; // 삭제할 구독 ID들
}