package com.chatapp.synk.chat.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Configuration class for setting up Redis caching.
 * This class defines the cache manager and the default cache configuration
 * for the application using Redis as the caching provider.
 * It enables caching support and configures the cache
 * manager to use a JSON serializer for cache values.
 * The cache entries will have a time-to-live (TTL) of 5 minutes,
 * and null values will not be cached.
 */
@Configuration
@EnableCaching
public class RedisConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.redis.host}") // fallback to localhost if env variable not set
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;

    /**
     * Creates RedisConnectionFactory using host and port from environment variables.
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(new RedisStandaloneConfiguration(redisHost, redisPort));
    }

    /**
     * Creates a CacheManager bean that uses Redis as the caching provider.
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        logger.info("Initializing Redis CacheManager with default configuration.");
        return RedisCacheManager.builder(connectionFactory).cacheDefaults(redisCacheConfiguration()).build();
    }

    /**
     * Creates the default cache configuration for Redis.
     */
    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        if (logger.isDebugEnabled()) {
            logger.debug("Configuring RedisCacheConfiguration with TTL=5 minutes and JSON serialization.");
        }
        return RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }

    /**
     * Custom RedisTemplate bean to align with JSON serializer.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        logger.info("Creating RedisTemplate with JSON value serializer.");
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        if (logger.isDebugEnabled()) {
            logger.debug("RedisTemplate initialized with String key serializer and JSON value serializer.");
        }
        return template;
    }
}

