package com.notifyhub.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMessageSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Invoked by the Redis listener container whenever a message arrives
     * on the "notifications" channel. Deserializes the JSON payload and
     * routes it to the recipient's personal WebSocket topic.
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody());
            NotificationResponse notification = objectMapper.readValue(json, NotificationResponse.class);

            String destination = "/topic/notifications/" + notification.getRecipientId();
            messagingTemplate.convertAndSend(destination, notification);

            log.debug("Routed notification {} to WebSocket topic {}", notification.getId(), destination);
        } catch (Exception e) {
            log.error("Failed to process Redis message: {}", e.getMessage());
        }
    }
}
