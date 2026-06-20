package com.notifyhub.dto;

import com.notifyhub.model.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    @Builder.Default
    private String senderId = "system";

    @NotBlank(message = "Recipient ID is required")
    private String recipientId;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be under 255 characters")
    private String title;

    @NotBlank(message = "Message is required")
    @Size(max = 1000, message = "Message must be under 1000 characters")
    private String message;

    @Builder.Default
    private NotificationType type = NotificationType.INFO;
}
