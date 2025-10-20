package com.Dolmeng_E.user.domain.sharedCalendar.dto;

import lombok.Getter;
import java.util.List;
import java.util.UUID;

@Getter
public class SubscriptionCreateReqDto {
    private String workspaceId;
    private List<UUID> targetUserIdList; // 구독할 대상 유저
}