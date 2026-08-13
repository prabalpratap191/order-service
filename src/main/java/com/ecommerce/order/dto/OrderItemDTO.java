package com.ecommerce.order.dto;

import java.math.BigDecimal;

public record OrderItemDTO(
    Long orderItemId,
    Long catalogItemId,
    String itemName,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal totalPrice
) {}