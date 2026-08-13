package com.ecommerce.order.dto;

import com.ecommerce.order.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryDTO(
    Long orderId,
    String orderNumber,
    LocalDateTime orderDate,
    OrderStatus orderStatus,
    BigDecimal totalAmount,
    int itemCount
) {}