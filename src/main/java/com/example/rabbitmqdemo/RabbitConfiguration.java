package com.example.rabbitmqdemo;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;
import org.springframework.retry.interceptor.StatefulRetryOperationsInterceptor;


@Configuration
public class RabbitConfiguration {

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {

        return new RabbitAdmin(connectionFactory);
    }


    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {

        return new RabbitTemplate(connectionFactory);
    }


    @Bean
    public Queue myQueue() {

        return QueueBuilder.durable("myqueue").quorum().build();
    }


    @Bean
    public Exchange myExchange() {

        DirectExchange directExchange = new DirectExchange("myexchange", true, false);
        directExchange.setDelayed(true);

        return directExchange;
    }


    @Bean
    public Binding myBinding(Queue myQueue, Exchange myExchange) {

        return new Binding(myQueue.getName(), Binding.DestinationType.QUEUE, myExchange.getName(), "myrouting", null);

    }


    @Bean
    public StatefulRetryOperationsInterceptor interceptor() {

        return RetryInterceptorBuilder.stateful()
                .maxAttempts(5)
                .backOffOptions(1000, 2.0, 10000) // initialInterval, multiplier, maxInterval
                .build();
    }

}
