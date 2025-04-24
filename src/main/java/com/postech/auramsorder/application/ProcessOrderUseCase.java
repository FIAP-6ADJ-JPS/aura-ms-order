package com.postech.auramsorder.application;

import com.postech.auramsorder.adapter.dto.OrderRequestDTO;
import com.postech.auramsorder.adapter.dto.RequestStockReserveDTO;
import com.postech.auramsorder.domain.Order;
import com.postech.auramsorder.gateway.OrderRepository;
import com.postech.auramsorder.gateway.database.jpa.entity.OrderEntity;
import com.postech.auramsorder.gateway.order.OrderStatusService;
import com.postech.auramsorder.gateway.client.ClientService;
import com.postech.auramsorder.gateway.payment.PaymentService;
import com.postech.auramsorder.gateway.product.ProductService;
import com.postech.auramsorder.gateway.stock.StockService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ProcessOrderUseCase {


    private final OrderRepository orderRepository;
    private final ClientService clientService;
    private final ProductService productService;
    private final StockService stockService;
    private final PaymentService paymentService;
    private final OrderStatusService orderStatusService;
    private final ModelMapper modelMapper;

    public ProcessOrderUseCase(OrderRepository orderRepository, ClientService clientService,
                               ProductService productService, StockService stockService,
                               PaymentService paymentService, OrderStatusService orderStatusService,
                               ModelMapper modelMapper) {
        this.orderRepository = orderRepository;
        this.clientService = clientService;
        this.productService = productService;
        this.stockService = stockService;
        this.paymentService = paymentService;
        this.orderStatusService = orderStatusService;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public void process(OrderRequestDTO orderRequestDTO) {
        // Criar o pedido com status ABERTO
        Order order = orderStatusService.createOpenOrder(orderRequestDTO);
        OrderEntity orderEntity = modelMapper.map(order, OrderEntity.class);
        orderEntity = orderRepository.save(orderEntity);

        try {
            // Verificar cliente
            clientService.verifyClient(orderRequestDTO.getClientId());

            boolean productsAvailable = false;
            // Verificar produtos e estoque
            for(RequestStockReserveDTO request : orderRequestDTO.getItems()) {
                boolean existProduct = productService.verifyProduct(request.getSku());
                if(existProduct) {
                    productsAvailable = true;
                } else {
                    orderEntity.setStatus("FECHADO_SKU_INCONSISTENTE");
                    orderRepository.save(orderEntity);
                    return;
                }
            }

            boolean stockAvailable = stockService.reserveStock(orderRequestDTO.getItems());

            if (!stockAvailable) {
                orderEntity.setStatus("FECHADO_SEM_ESTOQUE");
                orderRepository.save(orderEntity);
                return;
            }

            boolean paymentSuccessful = paymentService.processPayment(modelMapper.map(orderEntity, Order.class));

            if (!paymentSuccessful) {
                stockService.releaseStock(orderRequestDTO.getItems());
                orderEntity.setStatus("FECHADO_SEM_CREDITO");
                orderRepository.save(orderEntity);
                return;
            }

            orderEntity.setStatus("FECHADO_COM_SUCESSO");
            orderRepository.save(orderEntity);

        } catch (Exception e) {
            handleFailure(modelMapper.map(orderEntity, Order.class), orderRequestDTO, e);
            throw new RuntimeException("Failed to process order", e);
        }
    }

    private void handleFailure(Order order, OrderRequestDTO orderRequestDTO, Exception e) {
        try {
            stockService.releaseStock(orderRequestDTO.getItems());
            paymentService.refundIfNecessary(order);

            OrderEntity orderEntity = modelMapper.map(order, OrderEntity.class);
            orderEntity.setStatus("ERRO");
            orderRepository.save(orderEntity);

            log.error("Order failed with error: {}", e.getMessage());
        } catch (Exception exceptionInHandling) {
            log.error("Failed to handle failure for order {}: {}", order.getId(), exceptionInHandling.getMessage());
        }
    }

}