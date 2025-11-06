package com.Dolmeng_E.user.domain.notification.dto;

import com.Dolmeng_E.user.domain.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationCreateReqDto {
    private String title;
    private String content;
    private String type;
    private List<UUID> userIdList = new ArrayList<>();
    private LocalDateTime sendAt;
    private String workspaceId;
    private String projectId;
    private String stoneId;
    private String taskId;
}
