package com.chatapp.synk.chat.websocket;

import com.chatapp.synk.chat.common.ChatMessage;
import com.chatapp.synk.chat.common.Json;
import com.chatapp.synk.chat.rabbitmq.ChatMessagePublisher;
import com.chatapp.synk.chat.redis.RedisSessionStore;
import com.chatapp.synk.enums.ChatWebSocketStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private final LocalWsSessionRegistry localWsSessionRegistry;
    private final RedisSessionStore redisSessionStore;
    private final ChatMessagePublisher chatMessagePublisher;
    private final ExecutorService taskExecutor;

    public ChatWebSocketHandler(LocalWsSessionRegistry localWsSessionRegistry,
                                RedisSessionStore redisSessionStore,
                                ChatMessagePublisher chatMessagePublisher,
                                ExecutorService taskExecutor) {
        this.localWsSessionRegistry = localWsSessionRegistry;
        this.redisSessionStore = redisSessionStore;
        this.chatMessagePublisher = chatMessagePublisher;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession wsSession) throws Exception {
        String userId = (String) wsSession.getAttributes().get("userId");
        String serverId = System.getProperty("server.id");
        String sessionId = wsSession.getId();

        if (userId == null) {
            logger.warn("[WS_CONNECT_REJECTED] | sessionId={} reason=Missing userId", sessionId);
            wsSession.close(CloseStatus.BAD_DATA);
            return;
        }

        WebSocketSession localSession = localWsSessionRegistry.get(userId);
        String storedRedisSessionId = redisSessionStore.getUserSessionId(userId);

        boolean hasLocalSession = (localSession != null && localSession.isOpen());
        boolean hasDifferentRedisSession = (storedRedisSessionId != null && !storedRedisSessionId.equals(sessionId));

        if (hasLocalSession || hasDifferentRedisSession) {
            logger.warn("[WS_CONNECT_REJECTED] | userId={} sessionId={} reason=Duplicate session", userId, sessionId);
            try {
                wsSession.close(CloseStatus.POLICY_VIOLATION.withReason("Duplicate session"));
            } catch (IOException e) {
                logger.error("[WS_CLOSE_FAILED] | userId={} sessionId={} error={}", userId, sessionId, e.getMessage(), e);
            }
            return;
        }

        localWsSessionRegistry.add(sessionId, userId, wsSession);
        redisSessionStore.saveUserSession(userId, serverId, sessionId);

        logger.info("[WS_CONNECTED] | userId={} sessionId={} serverId={}", userId, sessionId, serverId);

        wsSession.sendMessage(new TextMessage(String.format(
                "{\"type\":\"connected\",\"userId\":\"%s\",\"serverId\":\"%s\"}", userId, serverId)));
    }

    @Override
    public void handleTextMessage(WebSocketSession wsSession, TextMessage message) throws Exception {
        String userId = (String) wsSession.getAttributes().get("userId");
        String sessionId = wsSession.getId();
        String payload = message.getPayload();

        logger.debug("[WS_MESSAGE_RECEIVED] | userId={} sessionId={} payload={}", userId, sessionId, payload);

        try {
            CompletableFuture.runAsync(() -> {
                try {
                    ChatMessage chatMessage = Json.mapper().readValue(payload, ChatMessage.class);
                    if (ChatWebSocketStatus.HEARTBEAT.equals(chatMessage.getWsStatus())) {
                        redisSessionStore.updateLastActiveTimestamp(userId);
                        logger.debug("[WS_HEARTBEAT] | userId={} sessionId={}", userId, sessionId);
                        return;
                    } else if (ChatWebSocketStatus.CHAT.equals(chatMessage.getWsStatus())) {
                        chatMessage.setSentAt(Instant.now().toString());
                        chatMessage.setFromUserId(userId);
                    }

                    chatMessagePublisher.sendToUser(chatMessage);

                    logger.info("[WS_MESSAGE_PUBLISHED] | userId={} sessionId={} toUserId={}",
                            userId, sessionId, chatMessage.getToUserId());

                } catch (Exception ex) {
                    logger.error("[WS_MESSAGE_PROCESSING_FAILED] | userId={} sessionId={} payload={}",
                            userId, sessionId, payload, ex);
                    try {
                        wsSession.sendMessage(new TextMessage("{\"error\":\"Invalid message format or server error\"}"));
                    } catch (IOException ioEx) {
                        logger.error("[WS_ERROR_RESPONSE_FAILED] | userId={} sessionId={}", userId, sessionId, ioEx);
                    }
                }
            }, taskExecutor).exceptionally(ex -> {
                logger.error("[WS_ASYNC_TASK_FAILED] | userId={} sessionId={} payload={}",
                        userId, sessionId, payload, ex);
                return null;
            });

        } catch (Exception e) {
            logger.error("[WS_TASK_SUBMISSION_FAILED] | userId={} sessionId={} payload={}",
                    userId, sessionId, payload, e);
            wsSession.sendMessage(new TextMessage("{\"error\":\"Server error, please retry\"}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession wsSession, CloseStatus status) throws Exception {
        String userId = (String) wsSession.getAttributes().get("userId");
        String sessionId = wsSession.getId();

        if (userId != null) {
            localWsSessionRegistry.remove(userId);
            redisSessionStore.deleteUserSession(userId);
            logger.info("[WS_DISCONNECTED] | userId={} sessionId={} status={}", userId, sessionId, status);
        } else {
            logger.warn("[WS_DISCONNECTED_UNKNOWN] | sessionId={} status={}", sessionId, status);
        }

        try {
            if (wsSession.isOpen()) {
                wsSession.close(status);
            }
        } catch (Exception e) {
            logger.error("[WS_CLEANUP_FAILED] | userId={} sessionId={}", userId, sessionId, e);
        }
    }
}
