package com.chatapp.synk.chat.rabbitmq;

import com.chatapp.synk.chat.common.ChatUtil;
import com.chatapp.synk.chat.common.ServerIdProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitMQConfig {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQConfig.class);

    private final String serverId;

    public RabbitMQConfig() {
        String resolvedId = ServerIdProvider.getServerId();
        if (resolvedId == null || resolvedId.isEmpty()) {
            resolvedId = System.getProperty("server.id");
        }
        if (resolvedId == null || resolvedId.isEmpty()) {
            throw new IllegalStateException("serverId must be set (via ServerIdProvider or system property 'server.id')");
        }
        this.serverId = resolvedId;
        logger.debug("RabbitMQConfig initialized with serverId={}", this.serverId);
    }

    @Bean
    public DirectExchange chatExchange() {
        return new DirectExchange(ChatUtil.EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue serverQueue() {
        return new Queue(ChatUtil.buildQueueName(serverId), true, false, false);
    }

    @Bean
    public Binding serverBinding(Queue serverQueue, DirectExchange chatExchange) {
        return BindingBuilder.bind(serverQueue)
                .to(chatExchange)
                .with(ChatUtil.buildBindingKey(serverId));
    }
}

