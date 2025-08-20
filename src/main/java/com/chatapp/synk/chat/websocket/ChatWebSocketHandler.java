package com.chatapp.synk.chat.websocket;

import com.chatapp.synk.chat.common.ChatMessage;
import com.chatapp.synk.chat.common.Json;
import com.chatapp.synk.chat.rabbitmq.ChatMessagePublisher;
import com.chatapp.synk.chat.redis.RedisSessionStore;
import com.chatapp.synk.dto.MessageDTO;
import com.chatapp.synk.enums.MessageStatus;
import com.chatapp.synk.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final LocalWsSessionRegistry localWsSessionRegistry;//local session registery.
    private final RedisSessionStore redisSessionStore;//redis session store.
    private final ChatMessagePublisher chatMessagePublisher;
    private final MessageService messageService; // MessageService for additional message handling

    // Constructor injection
    public ChatWebSocketHandler(LocalWsSessionRegistry localWsSessionRegistry, RedisSessionStore redisSessionStore, ChatMessagePublisher chatMessagePublisher, MessageService messageService) {
        this.localWsSessionRegistry = localWsSessionRegistry;
        this.redisSessionStore = redisSessionStore;
        this.chatMessagePublisher = chatMessagePublisher;
        this.messageService = messageService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession wsSession) throws Exception {
        String userId = (String) wsSession.getAttributes().get("userId");
        String serverId = System.getProperty("server.id");

        if (userId == null) {
            logger.warn("Connection rejected: no userId attribute found for sessionId={}", wsSession.getId());
            wsSession.close(CloseStatus.BAD_DATA);
            return;
        }

        localWsSessionRegistry.add(wsSession.getId(), userId, wsSession);
        redisSessionStore.saveUserSession(userId, serverId, wsSession.getId());

        logger.info("WebSocket connection established for userId={} with sessionId={} on serverId={}", userId, wsSession.getId(), serverId);

        wsSession.sendMessage(new TextMessage("{\"type\":\"connected\",\"serverId\":\"" + serverId + "\"}"));
    }

    @Override
    public void handleTextMessage(WebSocketSession wsSession, TextMessage message) throws Exception {
        String userId = (String) wsSession.getAttributes().get("userId");
        String payload = message.getPayload();

        logger.info("Received message from sessionId={} userId={} -> payload={}", wsSession.getId(), userId, payload);

        try {
            // Parse JSON payload into ChatMessage
            ChatMessage chatMessage = Json.mapper().readValue(payload, ChatMessage.class);

            // Ensure 'fromUserId' matches the authenticated user
            chatMessage.setFromUserId(userId);

            logger.info("Processing chat message: fromUserId={} toUserId={} body={}", chatMessage.getFromUserId(), chatMessage.getToUserId(), chatMessage.getBody());
            //convert chatMessage to MessageDTO
            MessageDTO messageDTO = new MessageDTO();
            messageDTO.setSenderId(chatMessage.getFromUserId());
            messageDTO.setReceiverId(chatMessage.getToUserId());
            messageDTO.setContent(chatMessage.getBody());
            messageDTO.setConversationId(chatMessage.getConversationId());
            messageDTO.setMessageStatus(MessageStatus.SENT);
            // Create MessageDTO and save it
            messageService.createMessage(messageDTO);

            // Hand over to publisher for routing via RabbitMQ
            chatMessagePublisher.sendToUser(chatMessage);

        } catch (Exception e) {
            logger.error("Failed to process incoming message from userId={} payload={}", userId, payload, e);

            // Notify client about error
            wsSession.sendMessage(new TextMessage("{\"error\":\"Invalid message format or server error\"}"));
        }
    }


    @Override
    public void afterConnectionClosed(WebSocketSession wsSession, CloseStatus status) throws Exception {
        String userId = (String) wsSession.getAttributes().get("userId");

        if (userId != null) {
            // Cleanup local and distributed registries
            localWsSessionRegistry.remove(userId);
            redisSessionStore.deleteUserSession(userId);

            logger.info("WebSocket connection closed for userId={} sessionId={} with status={}",
                    userId, wsSession.getId(), status);
        } else {
            // Defensive: session had no userId
            logger.warn("WebSocket connection closed for unknown user, sessionId={} with status={}",
                    wsSession.getId(), status);
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