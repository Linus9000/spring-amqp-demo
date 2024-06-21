package com.example.rabbitmqdemo;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/")
@CommonsLog
public class RabbitController {

    private final RabbitTemplate rabbitTemplate;


    public RabbitController(RabbitTemplate rabbitTemplate) {

        this.rabbitTemplate = rabbitTemplate;
    }


    @GetMapping
    public ResponseEntity<String> sendMessage() {

        for (int i = 0; i < 500_000; i++) {
            try {
                this.sendMessage("myexchange", "myrouting", String.valueOf(i));
            } catch (AmqpException e) {
                log.error("Could not send message with id " + i, e);
            }
        }

        return ResponseEntity.noContent().build();
    }


    private void sendMessage(String exchange, String routingKey, Object content) {

        MessageProperties properties = new MessageProperties();
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

        Message message = new Message(content.toString().getBytes(), properties);

        this.rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }

}
