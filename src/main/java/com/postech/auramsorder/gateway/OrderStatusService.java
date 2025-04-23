package com.postech.auramsorder.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.postech.auramsorder.adapter.dto.OrderRequestDTO;
import com.postech.auramsorder.domain.Order;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class OrderStatusService {
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    public OrderStatusService(OrderRepository orderRepository, ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
    }

    public Order createOpenOrder(OrderRequestDTO orderRequestDTO) {
        Order order = new Order();
        order.setClientId(orderRequestDTO.getClientId());
        order.setDtCreate(LocalDateTime.now());
        order.setStatus("ABERTO");

        try {
            String itemsJson = objectMapper.writeValueAsString(orderRequestDTO.getItems());
            order.setItems(itemsJson);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao serealizar items do pedido", e);
        }

        BigDecimal totalAmount = calculateTotalAmount(orderRequestDTO);
        order.setTotalAmount(totalAmount);

        if (orderRequestDTO.getPaymentData() != null) {
            order.setPaymentCardNumber(orderRequestDTO.getPaymentData().getCreditCardNumber());
        }

        return orderRepository.save(order);
    }

    public void markAsSuccessfullyClosed(Order order) {
        order.setStatus("FECHADO_COM_SUCESSO");
        orderRepository.save(order);
    }

    public void markAsClosedOutOfStock(Order order) {
        order.setStatus("FECHADO_SEM_ESTOQUE");
        orderRepository.save(order);
    }

    public void markAsClosedOutOfCredit(Order order) {
        order.setStatus("FECHADO_SEM_CREDITO");
        orderRepository.save(order);
    }

    public void markAsError(Order order, String errorMessage) {
        order.setStatus("ERRO");
        orderRepository.save(order);
    }


    private BigDecimal calculateTotalAmount(OrderRequestDTO dto) {
        return dto.getItems().stream()
                .map(item -> new BigDecimal(item.getQuantity() * 100))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}