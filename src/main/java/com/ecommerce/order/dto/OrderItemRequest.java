package com.ecommerce.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record OrderItemRequest(
    @NotNull(message = "Catalog item ID is required")
    Long catalogItemId,

    @NotBlank(message = "Item name is required")
    String itemName,

    @Min(value = 1, message = "Quantity must be at least 1")
    int quantity,

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.01", message = "Unit price must be greater than zero")
    BigDecimal unitPrice
) {}