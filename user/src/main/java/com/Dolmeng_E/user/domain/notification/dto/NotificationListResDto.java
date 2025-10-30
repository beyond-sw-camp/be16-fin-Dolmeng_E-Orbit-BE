package com.Dolmeng_E.user.domain.notification.dto;

import com.Dolmeng_E.user.domain.notification.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationListResDto {
    private Long id;
    private String title;
    private String content;
    private String readStatus;
    private LocalDateTime createdAt;

    public static NotificationListResDto fromEntiry(Notification notification) {
        return NotificationListResDto.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .createdAt(notification.getCreatedAt())
                .readStatus(String.valueOf(notification.getReadStatus()))
                .build();
    }
}
