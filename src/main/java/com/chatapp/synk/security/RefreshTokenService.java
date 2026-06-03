package com.chatapp.synk.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to manage refresh tokens in-memory.
 * This stores active refresh tokens with their associated usernames.
 * In a production environment, consider using a database or Redis for token blacklist management.
 */
@Service
public class RefreshTokenService {
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);
    
    // In-memory store: refreshToken -> username
    private final Map<String, String> tokenStore = new ConcurrentHashMap<>();
    
    private final JwtUtil jwtUtil;

    public RefreshTokenService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * Save a refresh token for the given user
     *
     * @param token    the refresh token
     * @param username the username associated with the token
     */
    public void saveRefreshToken(String token, String username) {
        tokenStore.put(token, username);
        //currently saving in Map later we can move to DB or Redis for better performance and scalability.
        if (logger.isDebugEnabled()) {
            logger.debug("Refresh token saved for user: {}", username);
        }
    }

    /**
     * Validate refresh token
     *
     * @param token the refresh token to validate
     * @return true if token is valid and exists in store, false otherwise
     */
    public boolean validateRefreshToken(String token) {
        if (!tokenStore.containsKey(token)) {
            logger.warn("Refresh token not found in store");
            return false;
        }

        if (!jwtUtil.isRefreshTokenValid(token)) {
            logger.warn("Refresh token is expired or invalid");
            tokenStore.remove(token); // Remove expired token
            return false;
        }

        return true;
    }

    /**
     * Get username from refresh token
     *
     * @param token the refresh token
     * @return username if token is valid, null otherwise
     */
    public String getUsernameFromRefreshToken(String token) {
        if (!validateRefreshToken(token)) {
            return null;
        }
        return tokenStore.get(token);
    }

    /**
     * Revoke (blacklist) a refresh token
     *
     * @param token the refresh token to revoke
     */
    public void revokeRefreshToken(String token) {
        tokenStore.remove(token);
        if (logger.isDebugEnabled()) {
            logger.debug("Refresh token revoked");
        }
    }

    /**
     * Revoke all refresh tokens for a user
     *
     * @param username the username
     */
    public void revokeAllRefreshTokensForUser(String username) {
        tokenStore.entrySet().removeIf(entry -> entry.getValue().equals(username));
        if (logger.isDebugEnabled()) {
            logger.debug("All refresh tokens revoked for user: {}", username);
        }
    }
}
