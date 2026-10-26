package com.syncstock.supplier.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Khai báo lại Exchange, Queue và Binding tương tự inventory-service.
 * Cả 2 service phải khai báo để RabbitMQ tự tạo/kiểm tra topology khi khởi động.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME      = "syncstock.inventory.exchange";
    public static final String IMPORT_QUEUE       = "syncstock.supplier.import.queue";
    public static final String IMPORT_ROUTING_KEY = "inventory.import.completed";

    @Bean
    public TopicExchange inventoryExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue importCompletedQueue() {
        return new Queue(IMPORT_QUEUE, true);
    }

    @Bean
    public Binding importBinding(Queue importCompletedQueue, TopicExchange inventoryExchange) {
        return BindingBuilder.bind(importCompletedQueue)
                .to(inventoryExchange)
                .with(IMPORT_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
