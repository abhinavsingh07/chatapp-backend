package com.chatapp.synk.chat.common;

public final class ChatUtil {

    private ChatUtil() {
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }

    // =======================
    // Constants
    // =======================

    /**
     * Direct exchange for chat messages
     */
    public static final String EXCHANGE_NAME = "chat.direct.exchange";

    /**
     * Redis user key prefix: user:{userId}
     */
    public static final String USER_KEY_PREFIX = "user:";

    /**
     * Queue name prefix: server-queue.{serverId}
     */
    private static final String SERVER_QUEUE_PREFIX = "server-queue.";

    /**
     * Routing key prefix: server.{serverId}
     */
    private static final String SERVER_QUEUE_BINDING_KEY_PREFIX = "server.binding.key.";

    // =======================
    // Redis Helpers
    // =======================

    public static String buildUserKey(String userId) {
        return USER_KEY_PREFIX + userId;
    }

    // =======================
    // RabbitMQ Helpers
    // =======================

    public static String buildBindingKey(String serverId) {
        return SERVER_QUEUE_BINDING_KEY_PREFIX + serverId;
    }

    // =======================
    // RabbitMQ Helpers
    // =======================

    public static String buildQueueName(String serverId) {
        return SERVER_QUEUE_PREFIX + serverId;
    }
}
