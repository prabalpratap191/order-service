package com.ecommerce.order.mapper;

import com.ecommerce.order.dto.*;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
public class OrderMapper {

    public OrderDTO toDTO(Order entity) {
        Objects.requireNonNull(entity, "Order entity must not be null");
        List<OrderItemDTO> itemDTOs = entity.getItems() != null
            ? entity.getItems().stream().map(this::toItemDTO).toList()
            : Collections.emptyList();
        return new OrderDTO(
            entity.getOrderId(),
            entity.getCustomerId(),
            entity.getOrderNumber(),
            entity.getOrderDate(),
            entity.getOrderStatus(),
            entity.getSubtotalAmount(),
            entity.getTaxAmount(),
            entity.getShippingAmount(),
            entity.getTotalAmount(),
            entity.getShippingAddress(),
            entity.getBillingAddress(),
            itemDTOs
        );
    }

    public OrderItemDTO toItemDTO(OrderItem entity) {
        Objects.requireNonNull(entity, "OrderItem entity must not be null");
        return new OrderItemDTO(
            entity.getOrderItemId(),
            entity.getCatalogItemId(),
            entity.getItemName(),
            entity.getQuantity(),
            entity.getUnitPrice(),
            entity.getTotalPrice()
        );
    }

    public OrderSummaryDTO toSummaryDTO(Order entity) {
        Objects.requireNonNull(entity, "Order entity must not be null");
        int itemCount = entity.getItems() != null ? entity.getItems().size() : 0;
        return new OrderSummaryDTO(
            entity.getOrderId(),
            entity.getOrderNumber(),
            entity.getOrderDate(),
            entity.getOrderStatus(),
            entity.getTotalAmount(),
            itemCount
        );
    }

    public Order toEntity(CreateOrderRequest request) {
        Objects.requireNonNull(request, "CreateOrderRequest must not be null");
        Order order = new Order();
        order.setCustomerId(request.customerId());
        order.setShippingAddress(request.shippingAddress());
        order.setBillingAddress(request.billingAddress());
        return order;
    }

    public OrderItem toItemEntity(OrderItemRequest request) {
        Objects.requireNonNull(request, "OrderItemRequest must not be null");
        OrderItem item = new OrderItem();
        item.setCatalogItemId(request.catalogItemId());
        item.setItemName(request.itemName());
        item.setQuantity(request.quantity());
        item.setUnitPrice(request.unitPrice());
        item.setTotalPrice(request.unitPrice().multiply(java.math.BigDecimal.valueOf(request.quantity())));
        return item;
    }
}