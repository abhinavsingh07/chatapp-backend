package com.chatapp.synk.chat.rabbitmq;

import com.chatapp.synk.chat.common.DeliveryEnvelope;
import com.chatapp.synk.chat.common.Json;
import com.chatapp.synk.chat.redis.RedisSessionStore;
import com.chatapp.synk.chat.websocket.LocalWsSessionRegistry;
import com.chatapp.synk.dto.MessageDTO;
import com.chatapp.synk.enums.MessageStatus;
import com.chatapp.synk.service.MessageService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Component
public class ChatMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageListener.class);
    private final MessageService messageService; // MessageService for additional message handling
    private final LocalWsSessionRegistry localWsSessionRegistry;
    private final RedisSessionStore redisSessionStore;

    public ChatMessageListener(MessageService messageService, LocalWsSessionRegistry localWsSessionRegistry, RedisSessionStore redisSessionStore) {
        this.messageService = messageService;
        this.localWsSessionRegistry = localWsSessionRegistry;
        this.redisSessionStore = redisSessionStore;
    }

    @RabbitListener(queues = "#{serverQueue.name}")
    public void onMessage(String payload, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        logger.debug("Received message payload: {}", payload);

        try {
            DeliveryEnvelope env = Json.mapper().readValue(payload, DeliveryEnvelope.class);

            // Persist the message first
            MessageDTO messageDTO = new MessageDTO();
            messageDTO.setSenderId(env.getMessage().getFromUserId());
            messageDTO.setReceiverId(env.getMessage().getToUserId());
            messageDTO.setContent(env.getMessage().getBody());
            messageDTO.setConversationId(env.getMessage().getConversationId());
            messageDTO.setMessageStatus(MessageStatus.SENT);
            //save to db
            messageService.saveMessage(messageDTO);
            logger.debug("Message saved to DB for conversationId={}", env.getMessage().getConversationId());

            // Try delivering to active session
            if (!trySend(env.getTargetSessionId(), env)) {
                String freshSessionId = redisSessionStore.getUserSessionId(env.getTargetUserId());
                if (!trySend(freshSessionId, env)) {
                    logger.warn("No active WebSocket session found for userId={}", env.getTargetUserId());
                }
            }

            // Acknowledge after success
            channel.basicAck(tag, false);//basicAck = "DB save + delivery done → remove message."

        } catch (Exception e) {
            logger.error("Failed to process incoming RabbitMQ message payload: {}", payload, e);
            try {
                //Don’t ack, requeue message
                channel.basicNack(tag, false, true);//basicNack(..., true) = "Something failed → put message back into queue for retry."
            } catch (IOException ioEx) {
                logger.error("Failed to nack message", ioEx);
            }
        }
    }

    /**
     * Attempts to send a message to a sessionId (if valid and open).
     *
     * @return true if sent successfully, false otherwise
     */
    private boolean trySend(String sessionId, DeliveryEnvelope env) {
        if (sessionId == null) {
            return false;
        }

        WebSocketSession wsSession = localWsSessionRegistry.getWSSession(sessionId);
        if (wsSession != null && wsSession.isOpen()) {
            try {
                wsSession.sendMessage(new TextMessage(Json.mapper().writeValueAsString(env.getMessage())));
                logger.info("Delivered message to userId={} via sessionId={}", env.getTargetUserId(), sessionId);
                return true;
            } catch (Exception e) {
                logger.error("Failed to send message to userId={} via sessionId={}", env.getTargetUserId(), sessionId, e);
            }
        } else {
            logger.debug("SessionId={} not found or closed for userId={}", sessionId, env.getTargetUserId());
        }
        return false;
    }

}