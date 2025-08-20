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
            logger.error("No serverId found! RabbitMQ beans may not function correctly.");
            throw new IllegalStateException("serverId must be set (via ServerIdProvider or system property 'server.id')");
        }
        this.serverId = resolvedId;
        logger.info("Using serverId={}", this.serverId);
    }

    @Bean
    public DirectExchange chatExchange() {
        logger.info("Creating DirectExchange with name={}", ChatUtil.EXCHANGE_NAME);
        return new DirectExchange(ChatUtil.EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue serverQueue() {
        String queueName = ChatUtil.buildQueueName(serverId) ;
        logger.info("Creating serverQueue with name={}", queueName);
        return new Queue(queueName, true, false, false);
    }

    @Bean
    public Binding serverBinding(Queue serverQueue, DirectExchange chatExchange) {
        String bindingKey = ChatUtil.buildBindingKey(serverId);
        logger.info("Creating Binding for queue={} to exchange={} with bindingkey={}",
                serverQueue.getName(), chatExchange.getName(), bindingKey);
        return BindingBuilder.bind(serverQueue).to(chatExchange).with(bindingKey);
    }
}
