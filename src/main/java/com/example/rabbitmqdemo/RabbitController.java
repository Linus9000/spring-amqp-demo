package com.example.rabbitmqdemo;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;


@RestController
@RequestMapping("/")
@CommonsLog
public class RabbitController {

    private final RabbitTemplate rabbitTemplate;

    //private final Map<String, String> messagesToSend = new HashMap<>();
    private final Set<String> messagesDelivered = new HashSet<>();
    private final Set<String> messagesFailed = new HashSet<>();
    private final Set<String> messagesReceived = new HashSet<>();


    public RabbitController(RabbitTemplate rabbitTemplate) {

        this.rabbitTemplate = rabbitTemplate;
    }


    @GetMapping
    public ResponseEntity<String> sendMessage(@RequestParam(value = "count", defaultValue = "500000") int count) {

        log.info("Sending %s messages...".formatted(count));

        //this.rabbitTemplate.setChannelTransacted(true);

        this.rabbitTemplate.setConfirmCallback((cd, ack, cause) -> {
            if (!ack) {
                if (cd != null) {
                    log.info("Nack! Correlation Data: %s - Cause: %s".formatted(cd, cause));
                }
            }
        });

        for (int i = 0; i < count; i++) {
            if (i % 1000 == 0) {
                log.info("%s messages sent...".formatted(i));
            }
            String msg = String.valueOf(i);
            try {
                this.sendMessage("myexchange", "myrouting", msg);
                this.messagesDelivered.add(msg);
            } catch (AmqpException e) {
                log.error("Could not send message with id " + i, e);
                this.messagesFailed.add(msg);
            }
        }

        String status = "Sent %s messages in total. Failed count: %s".formatted(this.messagesDelivered.size(), this.messagesFailed.size());


        log.info(status);

        return ResponseEntity.ok(status);
    }


    private void sendMessage(String exchange, String routingKey, Object content) {

        MessageProperties properties = new MessageProperties();
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

        Message message = new Message(content.toString().getBytes(), properties);

        CorrelationData cd = new CorrelationData(content.toString());

        this.rabbitTemplate.convertAndSend(exchange, routingKey, message, cd);

    }


    @RabbitListener(queues = "myqueue", id = "mylistener")
    public void receiveMessage(Message message) {

        this.messagesReceived.add(new String(message.getBody()));

    }


    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {

        List<String> messagesDeliveredButNotReceived = this.messagesDelivered.stream()
                .filter(Predicate.not(this.messagesReceived::contains))
                .toList();

        List<String> duplicates = listDuplicateUsingSet(this.messagesReceived);

        String status = """
                Messages delivered: %s
                Messages failed: %s
                Messages received: %s
                Messages delivered but not received: %s
                Messages received multiple times: %s
                """.formatted(
                this.messagesDelivered.size(),
                this.messagesFailed.size(),
                this.messagesReceived.size(),
                messagesDeliveredButNotReceived.size(),
                duplicates.size()
        );

        return ResponseEntity.ok(status);
    }


    List<String> listDuplicateUsingSet(Set<String> list) {

        List<String> duplicates = new ArrayList<>();
        Set<String> set = new HashSet<>();
        for (String i : list) {
            if (set.contains(i)) {
                duplicates.add(i);
            } else {
                set.add(i);
            }
        }
        return duplicates;
    }

}
