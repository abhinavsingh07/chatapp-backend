package com.chatapp.synk.chat.websocket;

import com.chatapp.synk.chat.common.ChatMessage;
import com.chatapp.synk.chat.common.Json;
import com.chatapp.synk.chat.rabbitmq.ChatMessagePublisher;
import com.chatapp.synk.chat.redis.RedisSessionStore;
import com.chatapp.synk.enums.ChatWebSocketStatus;
import com.chatapp.synk.service.UserService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
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
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private final LocalWsSessionRegistry localWsSessionRegistry;
    private final RedisSessionStore redisSessionStore;
    private final ChatMessagePublisher chatMessagePublisher;
    private final ExecutorService taskExecutor;
    private final UserService userService;

    // Metrics
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final Counter messagesSent;
    private final Counter messagesReceived;

    public ChatWebSocketHandler(LocalWsSessionRegistry localWsSessionRegistry,
                                RedisSessionStore redisSessionStore,
                                ChatMessagePublisher chatMessagePublisher,
                                ExecutorService taskExecutor,
                                UserService userService,
                                MeterRegistry meterRegistry) {
        this.localWsSessionRegistry = localWsSessionRegistry;
        this.redisSessionStore = redisSessionStore;
        this.chatMessagePublisher = chatMessagePublisher;
        this.taskExecutor = taskExecutor;
        this.userService = userService;

        // Register WebSocket metrics
        // Gauge for active connections
        Gauge.builder("websocket.connections.active", activeConnections, AtomicInteger::get)
                .description("Number of active WebSocket connections")
                .register(meterRegistry);

        // Counter for total messages sent
        messagesSent = Counter.builder("websocket.messages.sent")
                .description("Total number of WebSocket messages sent")
                .register(meterRegistry);

        // Counter for total messages received
        messagesReceived = Counter.builder("websocket.messages.received")
                .description("Total number of WebSocket messages received")
                .register(meterRegistry);
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

        //storing userid and user session in local registry and redis for other server to know where to send message for this user.
        localWsSessionRegistry.add(sessionId, userId, wsSession);
        redisSessionStore.saveUserSession(userId, serverId, sessionId);

        logger.info("[WS_CONNECTED] | userId={} sessionId={} serverId={}", userId, sessionId, serverId);

        wsSession.sendMessage(new TextMessage(String.format(
                "{\"type\":\"connected\",\"userId\":\"%s\",\"serverId\":\"%s\"}", userId, serverId)));
        // Increment active connections metric
        activeConnections.incrementAndGet();
    }

    @Override
    public void handleTextMessage(WebSocketSession wsSession, TextMessage message) throws Exception {
        String userId = (String) wsSession.getAttributes().get("userId");//setting in WebSocketAuthHandshakeInterceptor
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
                        // Increment messages received metric
                        // Increment received counter
                        //Key takeaway
                        //messagesReceived → increment every time a client sends a message.
                        //messagesSent → increment only when your server actually sends a message to a client.

                        messagesReceived.increment();
                        chatMessage.setSentAt(Instant.now().toString());//other user will see this time when message arrive to them.
                        chatMessage.setFromUserId(userId);
                    }

                    chatMessagePublisher.sendToUser(chatMessage);
                    // Increment messages sent metric
                    messagesSent.increment();

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
            try {
                //internally takes out data from redis
                userService.updateLastSeen(userId);
            } catch (Exception ex) {
                logger.error("[WS_LAST_SEEN_UPDATE_FAILED] | userId={} sessionId={}", userId, sessionId, ex);
            }
            localWsSessionRegistry.remove(userId);
            redisSessionStore.deleteUserSession(userId);
            logger.info("[WS_DISCONNECTED] | userId={} sessionId={} status={}", userId, sessionId, status);
            // Decrement active connections metric
            activeConnections.decrementAndGet();
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
