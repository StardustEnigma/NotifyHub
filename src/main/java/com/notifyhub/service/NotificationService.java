package com.notifyhub.service;

import com.notifyhub.dto.NotificationRequest;
import com.notifyhub.dto.NotificationResponse;
import com.notifyhub.exception.ResourceNotFoundException;
import com.notifyhub.model.Notification;
import com.notifyhub.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final RedisMessagePublisher redisPublisher;

    /**
     * Creates a notification, persists it to PostgreSQL, and publishes
     * it to Redis so all app instances can deliver it via WebSocket.
     */
    @Transactional
    public NotificationResponse sendNotification(NotificationRequest request) {
        Notification notification = Notification.builder()
                .senderId(request.getSenderId() != null ? request.getSenderId() : "system")
                .recipientId(request.getRecipientId())
                .title(request.getTitle())
                .message(request.getMessage())
                .type(request.getType())
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification created: id={}, from={}, to={}", saved.getId(), saved.getSenderId(), saved.getRecipientId());

        NotificationResponse response = mapToResponse(saved);
        redisPublisher.publish(response);

        return response;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(String userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(String userId) {
        return notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        notification.setRead(true);
        Notification updated = notificationRepository.save(notification);
        log.debug("Notification marked as read: {}", notificationId);

        return mapToResponse(updated);
    }

    @Transactional
    public int markAllAsRead(String userId) {
        int count = notificationRepository.markAllAsReadByRecipientId(userId);
        log.info("Marked {} notifications as read for user {}", count, userId);
        return count;
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .senderId(notification.getSenderId())
                .recipientId(notification.getRecipientId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
