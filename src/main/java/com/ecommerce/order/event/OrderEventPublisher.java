package com.ecommerce.order.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String orderCreatedTopic;
    private final String orderUpdatedTopic;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                               @Value("${order.kafka.topic.order-created}") String orderCreatedTopic,
                               @Value("${order.kafka.topic.order-updated}") String orderUpdatedTopic) {
        this.kafkaTemplate = Objects.requireNonNull(kafkaTemplate);
        this.orderCreatedTopic = Objects.requireNonNull(orderCreatedTopic);
        this.orderUpdatedTopic = Objects.requireNonNull(orderUpdatedTopic);
    }

    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing OrderCreatedEvent for order: {}", event.orderNumber());
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(orderCreatedTopic, event.orderNumber(), event);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish OrderCreatedEvent for order: {}", event.orderNumber(), ex);
            } else {
                log.debug("OrderCreatedEvent published successfully for order: {}, partition: {}, offset: {}",
                    event.orderNumber(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });
    }

    public void publishOrderUpdated(OrderUpdatedEvent event) {
        log.info("Publishing OrderUpdatedEvent for order: {}, status: {} -> {}",
            event.orderNumber(), event.previousStatus(), event.newStatus());
        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(orderUpdatedTopic, event.orderNumber(), event);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish OrderUpdatedEvent for order: {}", event.orderNumber(), ex);
            } else {
                log.debug("OrderUpdatedEvent published successfully for order: {}, partition: {}, offset: {}",
                    event.orderNumber(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });
    }
}