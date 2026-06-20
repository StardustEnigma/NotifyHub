package com.notifyhub.controller;

import com.notifyhub.dto.ApiResponse;
import com.notifyhub.dto.NotificationRequest;
import com.notifyhub.dto.NotificationResponse;
import com.notifyhub.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationResponse>> sendNotification(
            @Valid @RequestBody NotificationRequest request) {

        NotificationResponse notification = notificationService.sendNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Notification sent", notification));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
            @PathVariable String userId) {

        List<NotificationResponse> notifications = notificationService.getNotifications(userId);
        return ResponseEntity.ok(ApiResponse.ok(notifications));
    }

    @GetMapping("/{userId}/unread")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnreadNotifications(
            @PathVariable String userId) {

        List<NotificationResponse> notifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(ApiResponse.ok(notifications));
    }

    @GetMapping("/{userId}/unread/count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@PathVariable String userId) {
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.ok(count));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable UUID notificationId) {

        NotificationResponse notification = notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(ApiResponse.ok("Marked as read", notification));
    }

    @PatchMapping("/{userId}/read-all")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(@PathVariable String userId) {
        int count = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.ok("All notifications marked as read", count));
    }
}
