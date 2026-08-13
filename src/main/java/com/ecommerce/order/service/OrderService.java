package com.ecommerce.order.service;

import com.ecommerce.order.dto.*;
import com.ecommerce.order.enums.OrderStatus;
import java.util.List;

public interface OrderService {
    OrderDTO createOrder(CreateOrderRequest request);
    OrderDTO getOrderById(Long orderId);
    List<OrderSummaryDTO> getOrdersByCustomerId(Long customerId);
    OrderDTO updateOrderStatus(Long orderId, UpdateOrderStatusRequest request);
    OrderDTO cancelOrder(Long orderId);
}