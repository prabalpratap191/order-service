package com.ecommerce.order.controller;

import com.ecommerce.order.dto.*;
import com.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order", description = "Order Management APIs - Legacy: CreateOrderCommand, OrderManagementFlowCommandExecutor")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = Objects.requireNonNull(orderService);
    }

    @PostMapping
    @Operation(summary = "Create a new order",
        description = "Legacy: CreateOrderCommand.perform() -> OrderManagementFlowCommandExecutor.execute(). "
            + "Validates customer, calculates totals (subtotal, tax, shipping), generates order number, sets status to PENDING.")
    public ResponseEntity<OrderDTO> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("POST /api/v1/orders - Creating order for customer: {}", request.customerId());
        OrderDTO createdOrder = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID",
        description = "Legacy: OrderManagementFlowCommandExecutor.execute() - Retrieves order with all line items.")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long orderId) {
        log.info("GET /api/v1/orders/{}", orderId);
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get orders by customer ID",
        description = "Legacy: OrderManagementFlowCommandExecutor.execute() - Retrieves all orders for a customer sorted by date descending.")
    public ResponseEntity<List<OrderSummaryDTO>> getOrdersByCustomerId(@PathVariable Long customerId) {
        log.info("GET /api/v1/orders/customer/{}", customerId);
        return ResponseEntity.ok(orderService.getOrdersByCustomerId(customerId));
    }

    @PutMapping("/{orderId}/status")
    @Operation(summary = "Update order status",
        description = "Legacy: OrderManagementFlowCommandExecutor.execute() - Updates order status following workflow: "
            + "PENDING -> PROCESSING -> SHIPPED -> DELIVERED -> COMPLETED. Publishes OrderUpdatedEvent to Kafka.")
    public ResponseEntity<OrderDTO> updateOrderStatus(@PathVariable Long orderId,
                                                      @Valid @RequestBody UpdateOrderStatusRequest request) {
        log.info("PUT /api/v1/orders/{}/status - New status: {}", orderId, request.status());
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, request));
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel an order",
        description = "Legacy: OrderManagementFlowCommandExecutor.execute() - Cancels order if status is PENDING or PROCESSING. "
            + "Publishes OrderUpdatedEvent to Kafka.")
    public ResponseEntity<OrderDTO> cancelOrder(@PathVariable Long orderId) {
        log.info("POST /api/v1/orders/{}/cancel", orderId);
        return ResponseEntity.ok(orderService.cancelOrder(orderId));
    }
}