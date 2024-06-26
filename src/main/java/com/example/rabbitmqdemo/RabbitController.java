package com.example.rabbitmqdemo;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmCallback;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import jakarta.servlet.http.HttpServletResponse;
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
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/")
@CommonsLog
public class RabbitController {


    private static final String EXCHANGE_NAME = "myexchange";
    private static final String DESTINATION_QUEUE_NAME = "myqueue";
    private static final String ROUTING_KEY = "myrouting";

    private final ConcurrentNavigableMap<Long, String> outstandingConfirms = new ConcurrentSkipListMap<>();
    Set<Integer> sentMessages = new HashSet<>();
    Set<Integer> failedMessages = new HashSet<>();


    @GetMapping
    public ResponseEntity<String> sendMessage(@RequestParam(value = "count", defaultValue = "500000") int count, HttpServletResponse response) throws IOException {

        CountDownLatch allMessagesConfirmed = new CountDownLatch(count);
        ConnectionFactory factory = new ConnectionFactory();

        factory.setUsername("guest");
        factory.setPassword("guest");
        factory.setVirtualHost("/");
        factory.setHost("localhost");
        factory.setPort(5672);

        String status = "";

        ConfirmCallback cleanOutstandingConfirms = (sequenceNumber, multiple) -> {
            if (multiple) {
                ConcurrentNavigableMap<Long, String> confirmed = this.outstandingConfirms.headMap(
                        sequenceNumber, true
                );
                for (int i = 0; i < confirmed.size(); i++)
                {
                    allMessagesConfirmed.countDown();
                }
                confirmed.clear();
            } else {
                this.outstandingConfirms.remove(sequenceNumber);
                allMessagesConfirmed.countDown();
            }
        };

        try (Connection conn = factory.newConnection()) {

            Channel channel = conn.createChannel();

            channel.confirmSelect();
            channel.addConfirmListener(cleanOutstandingConfirms, (sequenceNumber, multiple) -> {
                String body = this.outstandingConfirms.get(sequenceNumber);
                log.error(String.format(
                        "Message with body %s has been nack-ed. Sequence number: %d, multiple: %b%n",
                        body, sequenceNumber, multiple)
                );
                cleanOutstandingConfirms.handle(sequenceNumber, multiple);
            });
            channel.exchangeDeclare(EXCHANGE_NAME, "direct", true);
            // channel.queueDeclare(DESTINATION_QUEUE_NAME, true, false, false, Map.of("x-queue-type", "quorum"));
            channel.queueDeclare(DESTINATION_QUEUE_NAME, true, false, false, null);
            channel.queueBind(DESTINATION_QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY);

            log.info("Sending %s messages...".formatted(count));


            for (int i = 0; i < count; i++) {
                try {
                    String body = String.valueOf(i);
                    this.outstandingConfirms.put(channel.getNextPublishSeqNo(), body);
                    channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, true, new AMQP.BasicProperties.Builder().deliveryMode(2).build(), body.getBytes());
                    this.sentMessages.add(i);
                } catch (Exception e) {
                    //log.error("Could not send message with id " + i, e);
                    this.failedMessages.add(i);
                }
            }

            try
            {
                log.info("Waiting for %s messages to be confirmed...".formatted(allMessagesConfirmed.getCount()));
                if (allMessagesConfirmed.await(5, TimeUnit.SECONDS))
                {
                    log.info("SUCCESS - all messages have been confirmed.");
                }
                else
                {
                    log.warn("Not all messages have been confirmed after waiting 5 seconds!");
                }
            }
            catch (InterruptedException e)
            {
            }

            status = "Sent %s messages in total. Failed count: %s.".formatted(this.sentMessages.size(), this.failedMessages.size());

        } catch (Exception e) {
            log.error("Could not connect to RabbitMQ", e);
        }

        log.info(status);

        response.sendRedirect("/status");

        return ResponseEntity.ok().build();
    }


    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {

        return ResponseEntity.ok("Sent %s messages in total. Failed count: %s. Outstanding confirms: %s".formatted(this.sentMessages.size(), this.failedMessages.size(), this.outstandingConfirms.size()));
    }


    @GetMapping("/clear")
    public ResponseEntity<String> clear() {

        this.outstandingConfirms.clear();
        this.sentMessages.clear();
        this.failedMessages.clear();

        return ResponseEntity.ok("Cleared");
    }

}
