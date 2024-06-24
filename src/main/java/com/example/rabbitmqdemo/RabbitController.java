package com.example.rabbitmqdemo;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


@RestController
@RequestMapping("/")
@CommonsLog
public class RabbitController {


    @GetMapping
    public ResponseEntity<String> sendMessage(@RequestParam(value = "count", defaultValue = "500000") int count) {

        ConnectionFactory factory = new ConnectionFactory();

        factory.setUsername("guest");
        factory.setPassword("guest");
        factory.setVirtualHost("/");
        factory.setHost("localhost");
        factory.setPort(5672);

        String status = "";

        try (Connection conn = factory.newConnection()) {

            Channel channel = conn.createChannel();

            channel.confirmSelect();
            channel.exchangeDeclare("myexchange", "direct", true);
            channel.queueDeclare("myqueue", true, false, false, Map.of("x-queue-type", "quorum"));
            channel.queueBind("myqueue", "myexchange", "myrouting");

            log.info("Sending %s messages...".formatted(count));

            Set<Integer> sentMessages = new HashSet<>(count);
            Set<Integer> failedMessages = new HashSet<>();
            for (int i = 0; i < count; i++) {
                try {
                    this.sendMessage(channel, "myexchange", "myrouting", String.valueOf(i));
                    sentMessages.add(i);
                } catch (MessageSendException e) {
                    log.error("Could not send message with id " + i, e);
                    failedMessages.add(i);
                }
            }

            status = "Sent %s messages in total. Failed count: %s".formatted(sentMessages.size(), failedMessages.size());

        } catch (Exception e) {
            log.error("Could not connect to RabbitMQ", e);
        }




        log.info(status);

        return ResponseEntity.ok(status);
    }


    private void sendMessage(Channel channel, String exchange, String routingKey, Object content) throws MessageSendException{

        try {
            channel.basicPublish(exchange, routingKey, true, new AMQP.BasicProperties.Builder().deliveryMode(2).build(), content.toString().getBytes());
            channel.waitForConfirmsOrDie(30_000);
        } catch (Exception e) {
            throw new MessageSendException(e);

        }

    }

}
