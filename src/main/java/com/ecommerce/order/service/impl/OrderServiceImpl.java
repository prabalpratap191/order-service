package com.ecommerce.order.service.impl;

import com.ecommerce.order.dto.*;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.enums.OrderStatus;
import com.ecommerce.order.event.OrderCreatedEvent;
import com.ecommerce.order.event.OrderEventPublisher;
import com.ecommerce.order.event.OrderUpdatedEvent;
import com.ecommerce.order.exception.InvalidOrderStateException;
import com.ecommerce.order.exception.OrderCancellationException;
import com.ecommerce.order.exception.ResourceNotFoundException;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.service.OrderService;
import com.ecommerce.order.util.OrderNumberGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderEventPublisher orderEventPublisher;
    private final OrderNumberGenerator orderNumberGenerator;
    private final BigDecimal taxRate;
    private final BigDecimal defaultShippingCost;

    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderMapper orderMapper,
                            OrderEventPublisher orderEventPublisher,
                            OrderNumberGenerator orderNumberGenerator,
                            @Value("${order.tax-rate:0.08}") BigDecimal taxRate,
                            @Value("${order.default-shipping-cost:5.99}") BigDecimal defaultShippingCost) {
        this.orderRepository = Objects.requireNonNull(orderRepository);
        this.orderMapper = Objects.requireNonNull(orderMapper);
        this.orderEventPublisher = Objects.requireNonNull(orderEventPublisher);
        this.orderNumberGenerator = Objects.requireNonNull(orderNumberGenerator);
        this.taxRate = taxRate;
        this.defaultShippingCost = defaultShippingCost;
    }

    @Override
    @Transactional
    public OrderDTO createOrder(CreateOrderRequest request) {
        log.info("Creating order for customer ID: {}", request.customerId());

        Order order = orderMapper.toEntity(request);
        order.setOrderNumber(orderNumberGenerator.generate());
        order.setOrderStatus(OrderStatus.PENDING);

        for (OrderItemRequest itemRequest : request.items()) {
            OrderItem item = orderMapper.toItemEntity(itemRequest);
            order.addItem(item);
        }

        BigDecimal subtotal = order.getItems().stream()
            .map(OrderItem::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setSubtotalAmount(subtotal);

        BigDecimal tax = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        order.setTaxAmount(tax);

        order.setShippingAmount(defaultShippingCost);

        BigDecimal total = subtotal.add(tax).add(defaultShippingCost);
        order.setTotalAmount(total);

        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully: {}", savedOrder.getOrderNumber());

        OrderCreatedEvent event = OrderCreatedEvent.of(
            savedOrder.getOrderId(),
            savedOrder.getCustomerId(),
            savedOrder.getOrderNumber(),
            savedOrder.getOrderDate(),
            savedOrder.getTotalAmount(),
            savedOrder.getItems().size()
        );
        orderEventPublisher.publishOrderCreated(event);

        return orderMapper.toDTO(savedOrder);
    }

    @Override
    public OrderDTO getOrderById(Long orderId) {
        log.debug("Retrieving order with ID: {}", orderId);
        Order order = orderRepository.findByOrderId(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));
        return orderMapper.toDTO(order);
    }

    @Override
    public List<OrderSummaryDTO> getOrdersByCustomerId(Long customerId) {
        log.debug("Retrieving orders for customer ID: {}", customerId);
        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId)
            .stream()
            .map(orderMapper::toSummaryDTO)
            .toList();
    }

    @Override
    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        log.info("Updating order {} status to {}", orderId, request.status());
        Order order = orderRepository.findByOrderId(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));

        OrderStatus currentStatus = order.getOrderStatus();
        OrderStatus targetStatus = request.status();

        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new InvalidOrderStateException(currentStatus.name(), targetStatus.name());
        }

        String previousStatus = order.getOrderStatus().name();
        order.setOrderStatus(targetStatus);
        Order updatedOrder = orderRepository.save(order);
        log.info("Order {} status updated from {} to {}", orderId, previousStatus, targetStatus);

        OrderUpdatedEvent event = OrderUpdatedEvent.of(
            updatedOrder.getOrderId(),
            updatedOrder.getCustomerId(),
            updatedOrder.getOrderNumber(),
            previousStatus,
            targetStatus.name()
        );
        orderEventPublisher.publishOrderUpdated(event);

        return orderMapper.toDTO(updatedOrder);
    }

    @Override
    @Transactional
    public OrderDTO cancelOrder(Long orderId) {
        log.info("Cancelling order: {}", orderId);
        Order order = orderRepository.findByOrderId(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));

        if (!order.getOrderStatus().isCancellable()) {
            throw new OrderCancellationException(orderId, order.getOrderStatus().getDisplayName());
        }

        String previousStatus = order.getOrderStatus().name();
        order.setOrderStatus(OrderStatus.CANCELLED);
        Order cancelledOrder = orderRepository.save(order);
        log.info("Order {} cancelled successfully", orderId);

        OrderUpdatedEvent event = OrderUpdatedEvent.of(
            cancelledOrder.getOrderId(),
            cancelledOrder.getCustomerId(),
            cancelledOrder.getOrderNumber(),
            previousStatus,
            OrderStatus.CANCELLED.name()
        );
        orderEventPublisher.publishOrderUpdated(event);

        return orderMapper.toDTO(cancelledOrder);
    }
}