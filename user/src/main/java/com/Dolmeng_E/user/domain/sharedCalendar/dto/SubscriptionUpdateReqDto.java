package com.Dolmeng_E.user.domain.sharedCalendar.dto;

import lombok.Getter;
import java.util.List;
import java.util.UUID;

@Getter
public class SubscriptionUpdateReqDto {
    private List<String> subscriptionIdList; // 수정할 구독 ID들
    private List<UUID> newTargetUserIdList;  // 변경할 대상 유저들
}