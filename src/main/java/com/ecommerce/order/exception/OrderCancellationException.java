package com.ecommerce.order.exception;

public class OrderCancellationException extends RuntimeException {
    private final Long orderId;
    private final String currentStatus;

    public OrderCancellationException(Long orderId, String currentStatus) {
        super(String.format("Order %d cannot be cancelled. Current status: %s. Only PENDING or PROCESSING orders can be cancelled.", orderId, currentStatus));
        this.orderId = orderId;
        this.currentStatus = currentStatus;
    }

    public Long getOrderId() { return orderId; }
    public String getCurrentStatus() { return currentStatus; }
}