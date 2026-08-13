package com.ecommerce.order.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderCreatedEvent(
    Long orderId,
    Long customerId,
    String orderNumber,
    LocalDateTime orderDate,
    BigDecimal totalAmount,
    int itemCount,
    LocalDateTime eventTimestamp
) {
    public static OrderCreatedEvent of(Long orderId, Long customerId, String orderNumber,
                                        LocalDateTime orderDate, BigDecimal totalAmount, int itemCount) {
        return new OrderCreatedEvent(orderId, customerId, orderNumber, orderDate, totalAmount, itemCount, LocalDateTime.now());
    }
}