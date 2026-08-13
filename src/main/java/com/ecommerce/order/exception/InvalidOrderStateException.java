package com.ecommerce.order.exception;

public class InvalidOrderStateException extends RuntimeException {
    private final String currentState;
    private final String targetState;

    public InvalidOrderStateException(String currentState, String targetState) {
        super(String.format("Invalid order state transition from %s to %s", currentState, targetState));
        this.currentState = currentState;
        this.targetState = targetState;
    }

    public String getCurrentState() { return currentState; }
    public String getTargetState() { return targetState; }
}