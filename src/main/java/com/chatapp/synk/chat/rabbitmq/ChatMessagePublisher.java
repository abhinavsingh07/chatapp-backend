package com.chatapp.synk.chat.rabbitmq;

import com.chatapp.synk.chat.common.ChatMessage;
import com.chatapp.synk.chat.common.ChatUtil;
import com.chatapp.synk.chat.common.DeliveryEnvelope;
import com.chatapp.synk.chat.common.Json;
import com.chatapp.synk.chat.websocket.LocalWsSessionRegistry;
import com.chatapp.synk.exceptionHandler.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChatMessagePublisher {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessagePublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    private final LocalWsSessionRegistry localWsSessionRegistryRegistry;

    @Autowired
    public ChatMessagePublisher(RabbitTemplate rabbitTemplate, RedisTemplate<String, Object> redisTemplate, LocalWsSessionRegistry localWsSessionRegistryRegistry) {
        this.rabbitTemplate = rabbitTemplate;
        this.redisTemplate = redisTemplate;
        this.localWsSessionRegistryRegistry = localWsSessionRegistryRegistry;
    }

    public void sendToUser(ChatMessage msg) {
        String toUserId = msg.getToUserId();
        logger.debug("Preparing to send message to userId={}", toUserId);

        //  Step 1:  Redis + RabbitMQ
        String redisKey = ChatUtil.buildUserKey(toUserId);
        String serverId = (String) redisTemplate.opsForHash().get(redisKey, "serverId");

        if (serverId == null) {
            logger.warn("UserId={} is offline. Storing for later delivery or skipping as per design.", toUserId);
            return;
        }

        String targetSessionId = (String) redisTemplate.opsForHash().get(redisKey, "sessionId");
        logger.info("Resolved serverId={} and sessionId={} for userId={}", serverId, targetSessionId, toUserId);

        DeliveryEnvelope env = new DeliveryEnvelope(msg, toUserId, serverId, targetSessionId);
        try {
            String json = Json.mapper().writeValueAsString(env);
            String routingKey=ChatUtil.buildBindingKey(serverId);//for direct routing key==binding key
            rabbitTemplate.convertAndSend(ChatUtil.EXCHANGE_NAME, routingKey, json);//publish to queue
            logger.info("Published message to exchange={} with routingKey=server.{} for userId={}", ChatUtil.EXCHANGE_NAME, serverId, toUserId);
        } catch (Exception e) {
            logger.error("Failed to publish message for userId={}", toUserId, e);
            throw new ServiceException(e.getMessage());
        }
    }
}