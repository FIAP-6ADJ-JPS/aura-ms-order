package com.postech.auramsorder.application;

import com.postech.auramsorder.adapter.dto.OrderRequestDTO;
import com.postech.auramsorder.domain.Order;
import com.postech.auramsorder.gateway.OrderRepository;
import com.postech.auramsorder.gateway.OrderStatusService;
import com.postech.auramsorder.gateway.client.ClientService;
import com.postech.auramsorder.gateway.order.InventoryService;
import com.postech.auramsorder.gateway.payment.PaymentService;
import com.postech.auramsorder.gateway.product.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ProcessOrderUseCase {

    private final OrderRepository orderRepository;
    private final ClientService clientService;
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final OrderStatusService orderStatusService;

    public ProcessOrderUseCase(OrderRepository orderRepository, ClientService clientService, ProductService productService, InventoryService inventoryService, PaymentService paymentService, OrderStatusService orderStatusService) {
        this.orderRepository = orderRepository;
        this.clientService = clientService;
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;
        this.orderStatusService = orderStatusService;
    }

    @Transactional
    public void process(OrderRequestDTO orderRequestDTO) {
        // Criar o pedido com status ABERTO
        Order order = orderStatusService.createOpenOrder(orderRequestDTO);

        try {
            // Verificar cliente
            clientService.verifyClient(orderRequestDTO.getClientId());

            // Verificar produtos e estoque
            boolean stockAvailable = inventoryService.reserveStock(orderRequestDTO.getItems());

            if (!stockAvailable) {
                orderStatusService.markAsClosedOutOfStock(order);
                return;
            }

            boolean paymentSuccessful = paymentService.processPayment(order);

            if (!paymentSuccessful) {
                inventoryService.releaseStock(orderRequestDTO.getItems());
                orderStatusService.markAsClosedOutOfCredit(order);
                return;
            }

            orderStatusService.markAsSuccessfullyClosed(order);

        } catch (Exception e) {
            handleFailure(order, orderRequestDTO, e);
            throw new RuntimeException("Failed to process order", e);
        }
    }

    private void handleFailure(Order order, OrderRequestDTO orderRequestDTO, Exception e) {
        try {
            inventoryService.releaseStock(orderRequestDTO.getItems());
            paymentService.refundIfNecessary(order);
            orderStatusService.markAsError(order, e.getMessage());
        } catch (Exception exceptionInHandling) {
            log.error("Failed to handle failure for order {}: {}", order.getId(), exceptionInHandling.getMessage());
        }
    }

}