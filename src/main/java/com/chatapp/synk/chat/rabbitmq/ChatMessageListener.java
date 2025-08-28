package com.chatapp.synk.chat.rabbitmq;

import com.chatapp.synk.chat.common.ChatMessage;
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
            createInfoLog("[RabbitMQ] Converted payload into DeliveryEnvelope conversationId={} targetUserId={}", env.getMessage().getConversationId(), env.getTargetUserId(), env.getMessage());

            // Persist only chat messages
            if (env.getMessage().getWsStatus().equals(ChatWebSocketStatus.CHAT)) {
                MessageDTO messageDTO = new MessageDTO();
                messageDTO.setSenderId(env.getMessage().getFromUserId());
                messageDTO.setReceiverId(env.getMessage().getToUserId());
                messageDTO.setContent(env.getMessage().getBody());
                messageDTO.setConversationId(env.getMessage().getConversationId());
                messageDTO.setMessageStatus(MessageStatus.SENT);

                messageService.saveMessage(messageDTO);
                logger.info("[DB] Persisted message conversationId={} senderId={} receiverId={}", env.getMessage().getConversationId(), env.getMessage().getFromUserId(), env.getMessage().getToUserId());
            }

            // Attempt delivery
            if (!trySend(env.getTargetSessionId(), env)) {
                logger.debug("[WS] SessionId={} not active, checking Redis...", env.getTargetSessionId());

                String freshSessionId = redisSessionStore.getUserSessionId(env.getTargetUserId());
                if (!trySend(freshSessionId, env)) {
                    logger.warn("[WS] No active WebSocket session for userId={}", env.getTargetUserId());
                }
            }

            // Acknowledge after DB + delivery attempt
            channel.basicAck(tag, false);
            createInfoLog("[RabbitMQ] Message acked conversationId={} userId={}", env.getMessage().getConversationId(), env.getTargetUserId(), env.getMessage());

        } catch (Exception e) {
            logger.error("[RabbitMQ] Failed processing payload (hash={}): {}", payload.hashCode(), e.getMessage(), e);
            try {
                channel.basicNack(tag, false, true);
                logger.warn("[RabbitMQ] Message nacked & requeued (hash={})", payload.hashCode());
            } catch (IOException ioEx) {
                logger.error("[RabbitMQ] Failed to nack message (hash={})", payload.hashCode(), ioEx);
            }
        }
    }

    /**
     * Attempts to send a message to a WebSocket session.
     */
    private boolean trySend(String sessionId, DeliveryEnvelope env) {
        if (sessionId == null) {
            logger.debug("[WS] No sessionId for userId={}", env.getTargetUserId());
            return false;
        }

        WebSocketSession wsSession = localWsSessionRegistry.getWSSession(sessionId);
        if (wsSession != null && wsSession.isOpen()) {
            try {
                wsSession.sendMessage(new TextMessage(Json.mapper().writeValueAsString(env.getMessage())));
                createInfoLog("[WS] Delivered to userId={} sessionId={}", env.getTargetUserId(), sessionId, env.getMessage());
                return true;
            } catch (Exception e) {
                logger.error("[WS] Failed delivery userId={} sessionId={} error={}", env.getTargetUserId(), sessionId, e.getMessage());
            }
        } else {
            logger.debug("[WS] SessionId={} closed/missing for userId={}", sessionId, env.getTargetUserId());
        }
        return false;
    }

    /**
     * Info logs only for chat messages, avoids clutter for typing/other statuses.
     */
    private void createInfoLog(String template, Object... args) {
        ChatMessage chatMessage = (ChatMessage) args[args.length - 1];
        if (chatMessage.getWsStatus().equals(ChatWebSocketStatus.CHAT)) {
            logger.info(template, args);
        }
    }
}

