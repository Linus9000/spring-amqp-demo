package com.example.rabbitmqdemo;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@CommonsLog
public class RabbitConfiguration {

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {

        return new RabbitAdmin(connectionFactory);
    }


    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {

        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setReturnsCallback(msg -> log.error("Message returned: %s".formatted(msg)));

        return rabbitTemplate;
    }


    @Bean
    public Queue myQueue() {

        return QueueBuilder.durable("myqueue").quorum().build();
    }


    @Bean
    public Exchange myExchange() {

        DirectExchange directExchange = new DirectExchange("myexchange", true, false);

        return directExchange;
    }


    @Bean
    public Binding myBinding(Queue myQueue, Exchange myExchange) {

        return new Binding(myQueue.getName(), Binding.DestinationType.QUEUE, myExchange.getName(), "myrouting", null);

    }

}
