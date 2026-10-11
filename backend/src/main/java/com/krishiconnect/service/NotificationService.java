package com.krishiconnect.service;

import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishiconnect.dto.NotificationResponse;
import com.krishiconnect.dto.PageResponse;
import com.krishiconnect.entity.Notification;
import com.krishiconnect.entity.NotificationType;
import com.krishiconnect.exception.NotFoundException;
import com.krishiconnect.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    /** Joins the caller's transaction, so a notification is never sent for a change that rolled back. */
    @Transactional
    public void notify(Long userId, NotificationType type, String title, String message, String link,
                       Map<String, Object> data) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message.length() > 500 ? message.substring(0, 497) + "..." : message);
        n.setLink(link);
        try {
            n.setData(data == null ? null : objectMapper.writeValueAsString(data));
        } catch (JsonProcessingException e) {
            n.setData(null);
        }
        notificationRepository.save(n);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(Long userId, int page, int size) {
        var result = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        return PageResponse.of(result, n -> NotificationResponse.from(n, parse(n.getData())));
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(Long userId, Long id) {
        Notification n = notificationRepository.findById(id)
                .filter(x -> x.getUserId().equals(userId))
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        n.setRead(true);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllRead(userId);
    }

    private Map<String, Object> parse(String json) {
        if (json == null) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() { });
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }
}
