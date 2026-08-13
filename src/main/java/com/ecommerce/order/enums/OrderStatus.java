package com.ecommerce.order.enums;

public enum OrderStatus {
    PENDING("Pending"),
    PROCESSING("Processing"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    RETURN_REQUESTED("Return Requested"),
    RETURN_APPROVED("Return Approved"),
    REFUNDED("Refunded");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case PENDING -> target == PROCESSING || target == CANCELLED;
            case PROCESSING -> target == SHIPPED || target == CANCELLED;
            case SHIPPED -> target == DELIVERED;
            case DELIVERED -> target == COMPLETED || target == RETURN_REQUESTED;
            case RETURN_REQUESTED -> target == RETURN_APPROVED;
            case RETURN_APPROVED -> target == REFUNDED;
            default -> false;
        };
    }

    public boolean isCancellable() {
        return this == PENDING || this == PROCESSING;
    }
}