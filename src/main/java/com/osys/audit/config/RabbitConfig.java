package com.osys.audit.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置
 */
@Configuration
public class RabbitConfig {

    @Value("${audit.mq.exchange:audit.exchange}")
    private String exchange;

    @Value("${audit.mq.queue:audit-log-queue}")
    private String queue;

    @Value("${audit.mq.routing-key:audit.log}")
    private String routingKey;

    @Value("${audit.mq.dead-letter-exchange:audit.dlx.exchange}")
    private String deadLetterExchange;

    @Value("${audit.mq.dead-letter-queue:audit-log-dlq}")
    private String deadLetterQueue;

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory batchRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setBatchListener(true);
        factory.setBatchSize(50);
        factory.setReceiveTimeout(30000L);
        factory.setConsumerBatchEnabled(true);
        return factory;
    }

    @Bean
    public DirectExchange auditExchange() {
        return new DirectExchange(exchange, true, false);
    }

    @Bean
    public DirectExchange auditDlxExchange() {
        return new DirectExchange(deadLetterExchange, true, false);
    }

    @Bean
    public Queue auditQueue() {
        return QueueBuilder.durable(queue)
                .withArgument("x-dead-letter-exchange", deadLetterExchange)
                .withArgument("x-dead-letter-routing-key", deadLetterQueue)
                .build();
    }

    @Bean
    public Queue auditDeadLetterQueue() {
        return QueueBuilder.durable(deadLetterQueue).build();
    }

    @Bean
    public Binding auditBinding() {
        return BindingBuilder.bind(auditQueue())
                .to(auditExchange())
                .with(routingKey);
    }

    @Bean
    public Binding auditDlxBinding() {
        return BindingBuilder.bind(auditDeadLetterQueue())
                .to(auditDlxExchange())
                .with(deadLetterQueue);
    }
}
