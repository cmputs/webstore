package com.example.sapir.webstore.repository;

import com.example.sapir.webstore.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByCustomerOrderId(Long orderId);
}
