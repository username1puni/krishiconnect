package com.krishiconnect.dto;

import java.time.Instant;
import java.util.Map;

import com.krishiconnect.entity.Notification;
import com.krishiconnect.entity.NotificationType;

public record NotificationResponse(Long id, NotificationType type, String title, String message, String link,
                                   Map<String, Object> data, boolean read, Instant createdAt) {

    public static NotificationResponse from(Notification n, Map<String, Object> data) {
        return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getMessage(), n.getLink(), data,
                n.isRead(), n.getCreatedAt());
    }
}
