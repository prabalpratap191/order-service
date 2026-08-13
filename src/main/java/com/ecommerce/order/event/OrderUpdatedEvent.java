package com.ecommerce.order.event;

import java.time.LocalDateTime;

public record OrderUpdatedEvent(
    Long orderId,
    Long customerId,
    String orderNumber,
    String previousStatus,
    String newStatus,
    LocalDateTime eventTimestamp
) {
    public static OrderUpdatedEvent of(Long orderId, Long customerId, String orderNumber,
                                        String previousStatus, String newStatus) {
        return new OrderUpdatedEvent(orderId, customerId, orderNumber, previousStatus, newStatus, LocalDateTime.now());
    }
}