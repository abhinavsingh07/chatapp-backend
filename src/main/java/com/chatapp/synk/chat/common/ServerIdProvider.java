package com.chatapp.synk.chat.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class ServerIdProvider {
    private static final Logger logger = LoggerFactory.getLogger(ServerIdProvider.class);
    private static final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int LENGTH = 5;
    private static final String serverId;

    static {
        serverId = generateRandomId();
        System.setProperty("server.id", serverId);
        logger.info("Generated serverId: {}", serverId);
    }

    private static String generateRandomId() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(CHARSET.charAt(random.nextInt(CHARSET.length())));
        }
        return sb.toString();
    }

    public static String getServerId() {
        return serverId;
    }
}
