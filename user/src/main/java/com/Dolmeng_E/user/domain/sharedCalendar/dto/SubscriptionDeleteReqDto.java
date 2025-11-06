package com.Dolmeng_E.user.domain.sharedCalendar.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDeleteReqDto {
    private String workspaceId;
    private List<String> subscriptionIdList; // 삭제할 구독 ID들
}