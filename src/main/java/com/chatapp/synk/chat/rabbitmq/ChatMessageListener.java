package com.chatapp.synk.chat.rabbitmq;

import com.chatapp.synk.chat.common.DeliveryEnvelope;
import com.chatapp.synk.chat.common.Json;
import com.chatapp.synk.chat.redis.RedisSessionStore;
import com.chatapp.synk.chat.websocket.LocalWsSessionRegistry;
import com.chatapp.synk.dto.MessageDTO;
import com.chatapp.synk.enums.ChatWebSocketStatus;
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

    private final MessageService messageService;
    private final LocalWsSessionRegistry localWsSessionRegistry;
    private final RedisSessionStore redisSessionStore;

    public ChatMessageListener(MessageService messageService, LocalWsSessionRegistry localWsSessionRegistry, RedisSessionStore redisSessionStore) {
        this.messageService = messageService;
        this.localWsSessionRegistry = localWsSessionRegistry;
        this.redisSessionStore = redisSessionStore;
    }

    @RabbitListener(queues = "#{serverQueue.name}")
    public void onMessage(String payload, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        logger.debug("[RabbitMQ] Received raw payload: {}", payload);

        try {
            DeliveryEnvelope env = Json.mapper().readValue(payload, DeliveryEnvelope.class);
            logger.info("[RabbitMQ] Converted payload into DeliveryEnvelope for conversationId={} targetUserId={}", env.getMessage().getConversationId(), env.getTargetUserId());

            // Persist message if it’s a chat message
            if (env.getMessage().getWsStatus().equals(ChatWebSocketStatus.CHAT)) {
                MessageDTO messageDTO = new MessageDTO();
                messageDTO.setSenderId(env.getMessage().getFromUserId());
                messageDTO.setReceiverId(env.getMessage().getToUserId());
                messageDTO.setContent(env.getMessage().getBody());
                messageDTO.setConversationId(env.getMessage().getConversationId());
                messageDTO.setMessageStatus(MessageStatus.SENT);

                messageService.saveMessage(messageDTO);
                logger.info("[DB] Persisted message for conversationId={} senderId={} receiverId={}", env.getMessage().getConversationId(), env.getMessage().getFromUserId(), env.getMessage().getToUserId());
            }

            // Try delivery
            if (!trySend(env.getTargetSessionId(), env)) {
                logger.debug("[WS] SessionId={} not active, checking Redis for fresh session...", env.getTargetSessionId());

                String freshSessionId = redisSessionStore.getUserSessionId(env.getTargetUserId());
                if (!trySend(freshSessionId, env)) {
                    logger.warn("[WS] No active WebSocket session found for userId={}", env.getTargetUserId());
                }
            }

            // Ack only if DB save + delivery attempt done
            channel.basicAck(tag, false);
            logger.debug("[RabbitMQ] Message acked (conversationId={} userId={})", env.getMessage().getConversationId(), env.getTargetUserId());

        } catch (Exception e) {
            logger.error("[RabbitMQ] Failed to process payload: {}", payload, e);
            try {
                channel.basicNack(tag, false, true);
                logger.warn("[RabbitMQ] Message nacked and requeued (payloadHash={})", payload.hashCode());
            } catch (IOException ioEx) {
                logger.error("[RabbitMQ] Failed to nack message (payloadHash={})", payload.hashCode(), ioEx);
            }
        }
    }

    /**
     * Attempts to send a message to a WebSocket session.
     */
    private boolean trySend(String sessionId, DeliveryEnvelope env) {
        if (sessionId == null) {
            logger.debug("[WS] No sessionId provided for userId={}", env.getTargetUserId());
            return false;
        }

        WebSocketSession wsSession = localWsSessionRegistry.getWSSession(sessionId);
        if (wsSession != null && wsSession.isOpen()) {
            try {
                wsSession.sendMessage(new TextMessage(Json.mapper().writeValueAsString(env.getMessage())));
                logger.info("[WS] Message delivered to userId={} sessionId={}", env.getTargetUserId(), sessionId);
                return true;
            } catch (Exception e) {
                logger.error("[WS] Failed to send message to userId={} sessionId={}", env.getTargetUserId(), sessionId, e);
            }
        } else {
            logger.debug("[WS] SessionId={} not found or closed for userId={}", sessionId, env.getTargetUserId());
        }
        return false;
    }
}
