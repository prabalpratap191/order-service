package com.ecommerce.order.service;

import com.ecommerce.order.dto.*;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.enums.OrderStatus;
import com.ecommerce.order.event.OrderEventPublisher;
import com.ecommerce.order.exception.InvalidOrderStateException;
import com.ecommerce.order.exception.OrderCancellationException;
import com.ecommerce.order.exception.ResourceNotFoundException;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.service.impl.OrderServiceImpl;
import com.ecommerce.order.util.OrderNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderEventPublisher orderEventPublisher;
    @Mock private OrderNumberGenerator orderNumberGenerator;
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
            orderRepository, orderMapper, orderEventPublisher, orderNumberGenerator,
            new BigDecimal("0.08"), new BigDecimal("5.99"));
    }

    @Test
    void createOrder_shouldCreateAndReturnOrder() {
        OrderItemRequest itemReq = new OrderItemRequest(1L, "Widget", 2, new BigDecimal("10.00"));
        CreateOrderRequest request = new CreateOrderRequest(1L, List.of(itemReq), "123 Main St", "123 Main St");

        Order order = new Order();
        order.setCustomerId(1L);
        order.setShippingAddress("123 Main St");
        order.setBillingAddress("123 Main St");

        OrderItem item = new OrderItem();
        item.setCatalogItemId(1L);
        item.setItemName("Widget");
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("10.00"));
        item.setTotalPrice(new BigDecimal("20.00"));

        Order savedOrder = new Order();
        savedOrder.setOrderId(1L);
        savedOrder.setCustomerId(1L);
        savedOrder.setOrderNumber("ORD202607230001");
        savedOrder.setOrderStatus(OrderStatus.PENDING);
        savedOrder.setSubtotalAmount(new BigDecimal("20.00"));
        savedOrder.setTaxAmount(new BigDecimal("1.60"));
        savedOrder.setShippingAmount(new BigDecimal("5.99"));
        savedOrder.setTotalAmount(new BigDecimal("27.59"));
        savedOrder.setOrderDate(LocalDateTime.now());
        savedOrder.setItems(new ArrayList<>(List.of(item)));

        OrderItemDTO itemDTO = new OrderItemDTO(1L, 1L, "Widget", 2, new BigDecimal("10.00"), new BigDecimal("20.00"));
        OrderDTO expectedDTO = new OrderDTO(1L, 1L, "ORD202607230001", savedOrder.getOrderDate(),
            OrderStatus.PENDING, new BigDecimal("20.00"), new BigDecimal("1.60"),
            new BigDecimal("5.99"), new BigDecimal("27.59"), "123 Main St", "123 Main St", List.of(itemDTO));

        when(orderMapper.toEntity(request)).thenReturn(order);
        when(orderMapper.toItemEntity(itemReq)).thenReturn(item);
        when(orderNumberGenerator.generate()).thenReturn("ORD202607230001");
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderMapper.toDTO(savedOrder)).thenReturn(expectedDTO);

        OrderDTO result = orderService.createOrder(request);

        assertThat(result.orderId()).isEqualTo(1L);
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("27.59"));
        verify(orderEventPublisher).publishOrderCreated(any());
    }

    @Test
    void getOrderById_existingOrder_shouldReturnDTO() {
        Order order = new Order();
        order.setOrderId(1L);
        order.setOrderNumber("ORD202607230001");
        order.setItems(new ArrayList<>());

        OrderDTO expectedDTO = new OrderDTO(1L, 1L, "ORD202607230001", LocalDateTime.now(),
            OrderStatus.PENDING, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            null, null, List.of());

        when(orderRepository.findByOrderId(1L)).thenReturn(Optional.of(order));
        when(orderMapper.toDTO(order)).thenReturn(expectedDTO);

        OrderDTO result = orderService.getOrderById(1L);

        assertThat(result.orderNumber()).isEqualTo("ORD202607230001");
        verify(orderRepository).findByOrderId(1L);
    }

    @Test
    void getOrderById_nonExistingOrder_shouldThrowException() {
        when(orderRepository.findByOrderId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(999L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Order");
    }

    @Test
    void getOrdersByCustomerId_shouldReturnSummaryList() {
        Order order = new Order();
        order.setOrderId(1L);
        order.setOrderNumber("ORD202607230001");
        order.setItems(new ArrayList<>());

        OrderSummaryDTO summaryDTO = new OrderSummaryDTO(1L, "ORD202607230001", LocalDateTime.now(),
            OrderStatus.PENDING, new BigDecimal("27.59"), 2);

        when(orderRepository.findByCustomerIdOrderByOrderDateDesc(1L)).thenReturn(List.of(order));
        when(orderMapper.toSummaryDTO(order)).thenReturn(summaryDTO);

        List<OrderSummaryDTO> result = orderService.getOrdersByCustomerId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).orderNumber()).isEqualTo("ORD202607230001");
    }

    @Test
    void updateOrderStatus_validTransition_shouldUpdate() {
        Order order = new Order();
        order.setOrderId(1L);
        order.setCustomerId(1L);
        order.setOrderNumber("ORD202607230001");
        order.setOrderStatus(OrderStatus.PENDING);
        order.setItems(new ArrayList<>());

        Order updatedOrder = new Order();
        updatedOrder.setOrderId(1L);
        updatedOrder.setCustomerId(1L);
        updatedOrder.setOrderNumber("ORD202607230001");
        updatedOrder.setOrderStatus(OrderStatus.PROCESSING);
        updatedOrder.setItems(new ArrayList<>());

        OrderDTO expectedDTO = new OrderDTO(1L, 1L, "ORD202607230001", LocalDateTime.now(),
            OrderStatus.PROCESSING, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            null, null, List.of());

        when(orderRepository.findByOrderId(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);
        when(orderMapper.toDTO(updatedOrder)).thenReturn(expectedDTO);

        UpdateOrderStatusRequest statusRequest = new UpdateOrderStatusRequest(OrderStatus.PROCESSING);
        OrderDTO result = orderService.updateOrderStatus(1L, statusRequest);

        assertThat(result.orderStatus()).isEqualTo(OrderStatus.PROCESSING);
        verify(orderEventPublisher).publishOrderUpdated(any());
    }

    @Test
    void updateOrderStatus_invalidTransition_shouldThrowException() {
        Order order = new Order();
        order.setOrderId(1L);
        order.setOrderStatus(OrderStatus.COMPLETED);

        when(orderRepository.findByOrderId(1L)).thenReturn(Optional.of(order));

        UpdateOrderStatusRequest statusRequest = new UpdateOrderStatusRequest(OrderStatus.PENDING);

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, statusRequest))
            .isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    void cancelOrder_pendingOrder_shouldCancel() {
        Order order = new Order();
        order.setOrderId(1L);
        order.setCustomerId(1L);
        order.setOrderNumber("ORD202607230001");
        order.setOrderStatus(OrderStatus.PENDING);
        order.setItems(new ArrayList<>());

        Order cancelledOrder = new Order();
        cancelledOrder.setOrderId(1L);
        cancelledOrder.setCustomerId(1L);
        cancelledOrder.setOrderNumber("ORD202607230001");
        cancelledOrder.setOrderStatus(OrderStatus.CANCELLED);
        cancelledOrder.setItems(new ArrayList<>());

        OrderDTO expectedDTO = new OrderDTO(1L, 1L, "ORD202607230001", LocalDateTime.now(),
            OrderStatus.CANCELLED, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            null, null, List.of());

        when(orderRepository.findByOrderId(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(cancelledOrder);
        when(orderMapper.toDTO(cancelledOrder)).thenReturn(expectedDTO);

        OrderDTO result = orderService.cancelOrder(1L);

        assertThat(result.orderStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderEventPublisher).publishOrderUpdated(any());
    }

    @Test
    void cancelOrder_shippedOrder_shouldThrowException() {
        Order order = new Order();
        order.setOrderId(1L);
        order.setOrderStatus(OrderStatus.SHIPPED);

        when(orderRepository.findByOrderId(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(1L))
            .isInstanceOf(OrderCancellationException.class);
    }
}