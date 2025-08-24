package com.chatapp.synk.chat.websocket;

import com.chatapp.synk.chat.common.ChatMessage;
import com.chatapp.synk.chat.common.Json;
import com.chatapp.synk.chat.rabbitmq.ChatMessagePublisher;
import com.chatapp.synk.chat.redis.RedisSessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final LocalWsSessionRegistry localWsSessionRegistry;//local session registery.
    private final RedisSessionStore redisSessionStore;//redis session store.
    private final ChatMessagePublisher chatMessagePublisher;


    private final ExecutorService taskExecutor;

    // Constructor injection
    public ChatWebSocketHandler(LocalWsSessionRegistry localWsSessionRegistry, RedisSessionStore redisSessionStore, ChatMessagePublisher chatMessagePublisher, ExecutorService taskExecutor) {
        this.localWsSessionRegistry = localWsSessionRegistry;
        this.redisSessionStore = redisSessionStore;
        this.chatMessagePublisher = chatMessagePublisher;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession wsSession) throws Exception {
        String userId = (String) wsSession.getAttributes().get("userId");//contains the same map you populated in beforeHandshake().
        String serverId = System.getProperty("server.id");

        if (userId == null) {
            logger.warn("Connection rejected: no userId attribute found for sessionId={}", wsSession.getId());
            wsSession.close(CloseStatus.BAD_DATA);
            return;
        }

        localWsSessionRegistry.add(wsSession.getId(), userId, wsSession);//this for local lookup
        redisSessionStore.saveUserSession(userId, serverId, wsSession.getId());//stores serverId and sessionId in redis for a given userid.

        logger.debug("WebSocket connection established for userId={} on serverId={}", userId, serverId);
        wsSession.sendMessage(new TextMessage("{\"type\":\"connected\",\"userId\":\"" + userId + "\"  ,\"serverId\":\"" + serverId + "\"}"));
    }

    @Override
    public void handleTextMessage(WebSocketSession wsSession, TextMessage message) throws Exception {
        String userId = (String) wsSession.getAttributes().get("userId");
        String payload = message.getPayload();

        logger.debug("Received message from sessionId={} userId={} -> payload={}", wsSession.getId(), userId, payload);

        try {
            CompletableFuture.runAsync(() -> {
                try {
                    ChatMessage chatMessage = Json.mapper().readValue(payload, ChatMessage.class);
                    chatMessage.setFromUserId(userId);
                    chatMessagePublisher.sendToUser(chatMessage);//call ChatMessagePublisher from there publishing happens to rabbitmq
                    logger.debug("Chat message published to RabbitMQ for userId={}", userId);
                } catch (Exception ex) {
                    logger.error("Failed to process incoming WS payload userId={} payload={}", userId, payload, ex);
                    try {
                        //nforming client something not working
                        wsSession.sendMessage(new TextMessage("{\"error\":\"Invalid message format or server error\"}"));
                    } catch (IOException ioEx) {
                        logger.error("Failed to send error back to client", ioEx);
                    }
                }
            }, taskExecutor).exceptionally(ex -> {
                logger.error("Unhandled async exception for userId={} payload={}", userId, payload, ex);
                return null;
            });

        } catch (Exception e) {
            logger.error("Failed to submit async task for userId={} payload={}", userId, payload, e);
            wsSession.sendMessage(new TextMessage("{\"error\":\"Server error, please retry\"}"));
        }
    }


    @Override
    //when client calls socket.close()
    public void afterConnectionClosed(WebSocketSession wsSession, CloseStatus status) throws Exception {
        String userId = (String) wsSession.getAttributes().get("userId");

        if (userId != null) {
            // Cleanup local and distributed registries
            localWsSessionRegistry.remove(userId);
            redisSessionStore.deleteUserSession(userId);
            logger.info("WebSocket connection closed for userId={} sessionId={} with status={}", userId, wsSession.getId(), status);
        } else {
            // Defensive: session had no userId
            logger.warn("WebSocket connection closed for unknown user, sessionId={} with status={}", wsSession.getId(), status);
        }

        try {
            // Ensure session is closed at the transport level
            if (wsSession.isOpen()) {
                wsSession.close(status);
            }
        } catch (Exception e) {
            logger.error("Error during cleanup of sessionId={} userId={}", wsSession.getId(), userId, e);
        }
    }
}