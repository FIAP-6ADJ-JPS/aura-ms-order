package com.postech.auramsorder.gateway;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import com.postech.auramsorder.domain.Order;
import com.postech.auramsorder.gateway.database.jpa.entity.OrderEntity;
import com.postech.auramsorder.gateway.database.jpa.repository.OrderJpaRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;
    private final ModelMapper modelMapper;

    @Override
    public Order save(Order order) {
        OrderEntity entity = toEntity(order);
        OrderEntity savedEntity = orderJpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public OrderEntity save_v2(OrderEntity orderEntity) {
        OrderEntity savedEntity = orderJpaRepository.save(orderEntity);
        return modelMapper.map(savedEntity, OrderEntity.class);
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orderJpaRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public List<Order> findAll() {
        return orderJpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findByStatus(String status) {
        return orderJpaRepository.findByStatus(status).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        orderJpaRepository.deleteById(id);
    }

    // Converter de Domínio para Entidade
    private OrderEntity toEntity(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.setId(order.getId());
        entity.setClientId(order.getClientId());
        entity.setItems(order.getItems());
        entity.setDtCreate(order.getDtCreate());
        entity.setStatus(order.getStatus());
        entity.setTotalAmount(order.getTotalAmount());
        entity.setPaymentId(order.getPaymentId());
        entity.setPaymentCardNumber(order.getPaymentCardNumber());
        return entity;
    }

    // Converter de Entidade para Domínio
    private Order toDomain(OrderEntity entity) {
        return new Order(
                entity.getId(),
                entity.getClientId(),
                entity.getItems(),
                entity.getDtCreate(),
                entity.getStatus(),
                entity.getTotalAmount(),
                entity.getPaymentId(),
                entity.getPaymentCardNumber()
        );
    }
}