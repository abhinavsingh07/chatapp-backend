package com.chatapp.synk.chat.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class RedisSessionCleaner {
    private static final Logger logger = LoggerFactory.getLogger(RedisSessionCleaner.class);
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisSessionCleaner(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void cleanupAllUserSessions() {
        String pattern = "user:*";
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            logger.info("Deleted all user sessions from redis before this restart of server,  keys size:{}: ", keys.size());
        }
    }
}
