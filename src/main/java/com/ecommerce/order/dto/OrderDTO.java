package com.ecommerce.order.dto;

import com.ecommerce.order.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDTO(
    Long orderId,
    Long customerId,
    String orderNumber,
    LocalDateTime orderDate,
    OrderStatus orderStatus,
    BigDecimal subtotalAmount,
    BigDecimal taxAmount,
    BigDecimal shippingAmount,
    BigDecimal totalAmount,
    String shippingAddress,
    String billingAddress,
    List<OrderItemDTO> items
) {}