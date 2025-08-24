package com.chatapp.synk.chat.redis;

import com.chatapp.synk.chat.common.ChatUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class RedisSessionStore {

    private static final Logger logger = LoggerFactory.getLogger(RedisSessionStore.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisSessionStore(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveUserSession(String userId, String serverId, String wsSessionId) {
        String redisLookupKey = ChatUtil.buildUserKey(userId);//"user:{userid}"

        Map<String, Object> map = new HashMap<>();
        map.put("serverId", serverId);
        map.put("sessionId", wsSessionId);

        redisTemplate.opsForHash().putAll(redisLookupKey, map);//"user:{userId}" → { serverId: "abc123", sessionId: "xyz789" }
        logger.info("Saved session for userId={} with serverId={} and sessionId={}", userId, serverId, wsSessionId);
    }

    public Map<Object, Object> getUserSession(String userId) {
        String redisLookupKey = ChatUtil.buildUserKey(userId);
        Map<Object, Object> sessionData = redisTemplate.opsForHash().entries(redisLookupKey);
        logger.debug("Fetched session data for userId={}: {}", userId, sessionData);
        return sessionData;
    }

    public String getUserServerId(String userId) {
        String redisLookupKey = ChatUtil.buildUserKey(userId);//"user:{userid}"
        Object v = redisTemplate.opsForHash().get(redisLookupKey, "serverId");
        String serverId = v != null ? v.toString() : null;
        logger.debug("Lookup serverId for userId={} -> {}", userId, serverId);
        return serverId;
    }

    public String getUserSessionId(String userId) {
        String redisLookupKey = ChatUtil.buildUserKey(userId);//"user:{userid}"
        Object v = redisTemplate.opsForHash().get(redisLookupKey, "sessionId");
        String sessionId = v != null ? v.toString() : null;
        logger.debug("Lookup sessionId for userId={} -> {}", userId, sessionId);
        return sessionId;
    }

    public void deleteUserSession(String userId) {
        String redisLookupKey = ChatUtil.buildUserKey(userId);//"user:{userid}"
        redisTemplate.delete(redisLookupKey);
        logger.info("Deleted session for userId={} with redisLookupKey={}", userId, redisLookupKey);
    }
}