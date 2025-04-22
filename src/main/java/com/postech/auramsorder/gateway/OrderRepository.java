package com.postech.auramsorder.gateway;

import com.postech.auramsorder.domain.Order;
import com.postech.auramsorder.gateway.database.jpa.entity.OrderEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository {

    Order save(Order order);
    OrderEntity save_v2(OrderEntity orderEntity);
    Optional<Order> findById(Long id);

    List<Order> findAll();

    List<Order> findByStatus(String status);

    void deleteById(Long id);
}
