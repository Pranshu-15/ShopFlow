package com.shopflow.order.repository;

import com.shopflow.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(String userId);

    Optional<Order> findByOrderId(String orderId);
}
