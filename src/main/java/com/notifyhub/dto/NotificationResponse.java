package com.notifyhub.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.notifyhub.model.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationResponse implements Serializable {

    private UUID id;
    private String senderId;
    private String recipientId;
    private String title;
    private String message;
    private NotificationType type;
    private boolean read;
    private LocalDateTime createdAt;

    /**
     * Returns a human-readable relative timestamp like "2 minutes ago" or "just now".
     * Computed on the fly so it stays current without caching issues.
     */
    public String getTimeAgo() {
        if (createdAt == null) return "";

        LocalDateTime now = LocalDateTime.now();
        long seconds = ChronoUnit.SECONDS.between(createdAt, now);

        if (seconds < 60) return "just now";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        long hours = minutes / 60;
        if (hours < 24) return hours + (hours == 1 ? " hour ago" : " hours ago");
        long days = hours / 24;
        if (days < 30) return days + (days == 1 ? " day ago" : " days ago");
        long months = days / 30;
        return months + (months == 1 ? " month ago" : " months ago");
    }
}
