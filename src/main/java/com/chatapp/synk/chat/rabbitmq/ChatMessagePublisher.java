package com.chatapp.synk.chat.rabbitmq;

import com.chatapp.synk.chat.common.ChatMessage;
import com.chatapp.synk.chat.common.ChatUtil;
import com.chatapp.synk.chat.common.DeliveryEnvelope;
import com.chatapp.synk.chat.common.Json;
import com.chatapp.synk.chat.websocket.LocalWsSessionRegistry;
import com.chatapp.synk.enums.ChatWebSocketStatus;
import com.chatapp.synk.exceptionHandler.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChatMessagePublisher {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessagePublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final LocalWsSessionRegistry localWsSessionRegistryRegistry;

    public ChatMessagePublisher(RabbitTemplate rabbitTemplate, RedisTemplate<String, Object> redisTemplate, LocalWsSessionRegistry localWsSessionRegistryRegistry) {
        this.rabbitTemplate = rabbitTemplate;
        this.redisTemplate = redisTemplate;
        this.localWsSessionRegistryRegistry = localWsSessionRegistryRegistry;
    }

    public void sendToUser(ChatMessage chatMessage) {
        String toUserId = chatMessage.getToUserId();
        logger.debug("[PUBLISH] Preparing message delivery | toUserId={}", toUserId);

        // Step 1: Redis lookup for session info
        String redisKey = ChatUtil.buildUserKey(toUserId);
        String serverId = (String) redisTemplate.opsForHash().get(redisKey, "serverId");
        String targetSessionId = (String) redisTemplate.opsForHash().get(redisKey, "sessionId");

        if (serverId == null) {
            // user offline → fallback handling
            logger.info("[OFFLINE] Target user offline | toUserId={}. Persisting to DB for later delivery.", toUserId);
            serverId = System.getProperty("server.id");
        }

        try {
            DeliveryEnvelope env = new DeliveryEnvelope(chatMessage, toUserId, serverId, targetSessionId);
            String json = Json.mapper().writeValueAsString(env);
            String routingKey = ChatUtil.buildBindingKey(serverId);

            rabbitTemplate.convertAndSend(ChatUtil.EXCHANGE_NAME, routingKey, json);

            logger.info("[SUCCESS] Published message | exchange={} | routingKey={} | toUserId={} | sessionId={}", ChatUtil.EXCHANGE_NAME, routingKey, toUserId, targetSessionId);

        } catch (Exception e) {
            logger.error("[ERROR] Failed to publish message | toUserId={} | sessionId={} | reason={}", toUserId, targetSessionId, e.getMessage(), e);
            throw new ServiceException("Failed to publish chat message", e);
        }
    }

    private void createInfoLog(String log, ChatMessage chatMessage) {
        if (chatMessage.getWsStatus().equals(ChatWebSocketStatus.CHAT)) {
            logger.info("[CHAT] {}", log);
        }
    }
}