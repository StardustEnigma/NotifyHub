package com.notifyhub.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.config.RedisConfig;
import com.notifyhub.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisMessagePublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publishes a notification to the Redis Pub/Sub channel.
     * In a multi-instance deployment, every app instance receives this message
     * and routes it to locally connected WebSocket clients.
     */
    public void publish(NotificationResponse notification) {
        try {
            String payload = objectMapper.writeValueAsString(notification);
            redisTemplate.convertAndSend(RedisConfig.NOTIFICATION_CHANNEL, payload);
            log.debug("Published notification {} to Redis channel", notification.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize notification for Redis: {}", e.getMessage());
        }
    }
}
