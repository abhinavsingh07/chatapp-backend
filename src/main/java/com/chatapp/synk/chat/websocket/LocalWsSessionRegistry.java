package com.chatapp.synk.chat.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LocalWsSessionRegistry {

    private static final Logger logger = LoggerFactory.getLogger(LocalWsSessionRegistry.class);

    private final Map<String, WebSocketSession> wsSessions = new ConcurrentHashMap<>();
    private final Map<String, String> wsSessionToUser = new ConcurrentHashMap<>();

    public void add(String sessionId, String userId, WebSocketSession session) {
        wsSessionToUser.put(userId, sessionId);
        wsSessions.put(sessionId, session);
        logger.info("Added sessionId={} for userId={}", sessionId, userId);
    }

    public WebSocketSession get(String userId) {
        String wsSessionId = wsSessionToUser.get(userId);
        WebSocketSession session = wsSessionId != null ? wsSessions.get(wsSessionId) : null;
        logger.debug("Lookup WebSocketSession for sessionId={} -> {}", wsSessionId, (wsSessionId != null ? "FOUND" : "NOT FOUND"));
        return session;
    }

    public WebSocketSession getWSSession(String wsSessionId) {
        WebSocketSession session = wsSessions.get(wsSessionId);
        logger.debug("Lookup WebSocketSession for sessionId={} -> {}", wsSessionId, (wsSessionId != null ? "FOUND" : "NOT FOUND"));
        return session;
    }

    public String remove(String userId) {
        String wsSessionId = wsSessionToUser.get(userId);
        wsSessions.remove(wsSessionId);
        wsSessionToUser.remove(userId);
        if (userId != null) {
            logger.info("Removed sessionId={} for userId={}", wsSessionId, userId);
        } else {
            logger.warn("Attempted to remove unknown sessionId={}", wsSessionId);
        }
        return userId;
    }

    public Set<Map.Entry<String, String>> entries() {
        logger.debug("Retrieving all session entries, total count={}", wsSessionToUser.size());
        return wsSessionToUser.entrySet();
    }
}