package com.example.rabbitmqdemo;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.Set;


@RestController
@RequestMapping("/")
@CommonsLog
public class RabbitController {

    private final RabbitTemplate rabbitTemplate;


    public RabbitController(RabbitTemplate rabbitTemplate) {

        this.rabbitTemplate = rabbitTemplate;
    }


    @GetMapping
    public ResponseEntity<String> sendMessage(@RequestParam(value = "count", defaultValue = "500000") int count) {

        log.info("Sending %s messages...".formatted(count));

        Set<Integer> sentMessages = new HashSet<>(count);
        Set<Integer> failedMessages = new HashSet<>();
        for (int i = 0; i < count; i++) {
            try {
                this.sendMessage("myexchange", "myrouting", String.valueOf(i));
                sentMessages.add(i);
            } catch (AmqpException e) {
                log.error("Could not send message with id " + i, e);
                failedMessages.add(i);
            }
        }

        String status = "Sent %s messages in total. Failed count: %s".formatted(sentMessages.size(), failedMessages.size());

        log.info(status);

        return ResponseEntity.ok(status);
    }


    private void sendMessage(String exchange, String routingKey, Object content) {

        MessageProperties properties = new MessageProperties();
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

        Message message = new Message(content.toString().getBytes(), properties);

        this.rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }

}
