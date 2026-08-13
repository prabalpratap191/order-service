package com.ecommerce.order.controller;

import com.ecommerce.order.dto.*;
import com.ecommerce.order.enums.OrderStatus;
import com.ecommerce.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void createOrder_shouldReturnCreatedOrder() throws Exception {
        OrderItemRequest itemReq = new OrderItemRequest(1L, "Widget", 2, new BigDecimal("10.00"));
        CreateOrderRequest request = new CreateOrderRequest(1L, List.of(itemReq), "123 Main St", "123 Main St");

        OrderItemDTO itemDTO = new OrderItemDTO(1L, 1L, "Widget", 2, new BigDecimal("10.00"), new BigDecimal("20.00"));
        OrderDTO responseDTO = new OrderDTO(1L, 1L, "ORD202607230001", LocalDateTime.now(),
            OrderStatus.PENDING, new BigDecimal("20.00"), new BigDecimal("1.60"),
            new BigDecimal("5.99"), new BigDecimal("27.59"), "123 Main St", "123 Main St", List.of(itemDTO));

        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderId").value(1))
            .andExpect(jsonPath("$.orderStatus").value("PENDING"))
            .andExpect(jsonPath("$.orderNumber").value("ORD202607230001"));
    }

    @Test
    @WithMockUser
    void createOrder_invalidRequest_shouldReturn400() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(null, List.of(), "", "");

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void getOrderById_shouldReturnOrder() throws Exception {
        OrderDTO dto = new OrderDTO(1L, 1L, "ORD202607230001", LocalDateTime.now(),
            OrderStatus.PENDING, new BigDecimal("20.00"), new BigDecimal("1.60"),
            new BigDecimal("5.99"), new BigDecimal("27.59"), "123 Main St", "123 Main St", List.of());

        when(orderService.getOrderById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/orders/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(1))
            .andExpect(jsonPath("$.orderNumber").value("ORD202607230001"));
    }

    @Test
    @WithMockUser
    void getOrdersByCustomerId_shouldReturnList() throws Exception {
        OrderSummaryDTO summary = new OrderSummaryDTO(1L, "ORD202607230001", LocalDateTime.now(),
            OrderStatus.PENDING, new BigDecimal("27.59"), 2);

        when(orderService.getOrdersByCustomerId(1L)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/orders/customer/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].orderNumber").value("ORD202607230001"))
            .andExpect(jsonPath("$[0].itemCount").value(2));
    }

    @Test
    @WithMockUser
    void updateOrderStatus_shouldReturnUpdatedOrder() throws Exception {
        UpdateOrderStatusRequest statusRequest = new UpdateOrderStatusRequest(OrderStatus.PROCESSING);
        OrderDTO dto = new OrderDTO(1L, 1L, "ORD202607230001", LocalDateTime.now(),
            OrderStatus.PROCESSING, new BigDecimal("20.00"), new BigDecimal("1.60"),
            new BigDecimal("5.99"), new BigDecimal("27.59"), "123 Main St", "123 Main St", List.of());

        when(orderService.updateOrderStatus(eq(1L), any(UpdateOrderStatusRequest.class))).thenReturn(dto);

        mockMvc.perform(put("/api/v1/orders/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderStatus").value("PROCESSING"));
    }

    @Test
    @WithMockUser
    void cancelOrder_shouldReturnCancelledOrder() throws Exception {
        OrderDTO dto = new OrderDTO(1L, 1L, "ORD202607230001", LocalDateTime.now(),
            OrderStatus.CANCELLED, new BigDecimal("20.00"), new BigDecimal("1.60"),
            new BigDecimal("5.99"), new BigDecimal("27.59"), "123 Main St", "123 Main St", List.of());

        when(orderService.cancelOrder(1L)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/orders/1/cancel"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderStatus").value("CANCELLED"));
    }
}