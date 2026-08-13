package com.ecommerce.order.repository;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items"})
    Optional<Order> findByOrderId(Long orderId);

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByCustomerIdOrderByOrderDateDesc(Long customerId);

    List<Order> findByCustomerIdAndOrderStatus(Long customerId, OrderStatus orderStatus);
}