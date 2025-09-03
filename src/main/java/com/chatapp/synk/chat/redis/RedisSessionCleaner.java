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
        Set<String> keys = null;

        int retries = 5;
        while (retries > 0) {
            try {
                keys = redisTemplate.keys(pattern);
                break; // success
            } catch (Exception e) {
                retries--;
                logger.warn("Redis not ready yet. Retrying... attempts left: {}", retries);
                try {
                    Thread.sleep(2000); // wait 2 seconds before retry
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            logger.info("Deleted all user sessions from Redis, keys size: {}", keys.size());
        } else {
            logger.warn("No keys deleted or Redis unavailable after retries.");
        }

    }
}
