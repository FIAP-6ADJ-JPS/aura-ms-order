package com.postech.auramsorder.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.postech.auramsorder.adapter.dto.OrderRequestDTO;
import com.postech.auramsorder.adapter.dto.RequestStockReserveDTO;
import com.postech.auramsorder.domain.Order;
import com.postech.auramsorder.gateway.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
public class ProcessOrderUseCase {

    @Value("${client.service.url}")
    private String clientServiceUrl;

    @Value("${product.service.url}")
    private String productServiceUrl;

    @Value("${stock.service.url}")
    private String stockServiceUrl;

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public ProcessOrderUseCase(OrderRepository orderRepository, ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public void process(OrderRequestDTO orderRequestDTO) {
        try {
            validateOrderRequest(orderRequestDTO);

            Order order = createOrderFromDTO(orderRequestDTO);
            findClientById(orderRequestDTO.getClientId().toString());

            log.info("Processando pedido para o cliente: {}", orderRequestDTO.getClientId());
            order = orderRepository.save(order);
            log.info("Pedido salvo com o ID: {} e status: {}", order.getId(), order.getStatus());

            processOrderItems(orderRequestDTO);

            order.setStatus("FECHADO_COM_SUCESSO");
            orderRepository.save(order);
            log.info("Pedido processado com sucesso. Status final: {}", order.getStatus());

        } catch (Exception e) {
            log.error("Erro ao processar o pedido: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao processar o pedido", e);
        }
    }

    private void validateOrderRequest(OrderRequestDTO orderRequestDTO) {
        Optional.ofNullable(orderRequestDTO.getClientId())
                .orElseThrow(() -> new IllegalArgumentException("ID do cliente não pode ser nulo"));
        Optional.ofNullable(orderRequestDTO.getItems())
                .filter(items -> !items.isEmpty())
                .orElseThrow(() -> new IllegalArgumentException("Itens do pedido não podem ser nulos ou vazios"));
    }

    private void processOrderItems(OrderRequestDTO orderRequestDTO) {
        for (RequestStockReserveDTO item : orderRequestDTO.getItems()) {
            findProductBySku(item.getSku());
            findProductInStock(item);
        }
    }

    private void findClientById(String clientId) {
        String url = clientServiceUrl + clientId;
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            log.info("Cliente encontrado: {}", response);
        } catch (Exception e) {
            log.error("Erro ao buscar cliente: {}", e.getMessage(), e);
            throw new RuntimeException("Cliente não encontrado", e);
        }
    }

    private void findProductBySku(String sku) {
        String url = productServiceUrl + sku;
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            log.info("Produto encontrado: {}", response);
        } catch (Exception e) {
            log.error("Erro ao buscar produto: {}", e.getMessage(), e);
            throw new RuntimeException("Produto não encontrado", e);
        }
    }

    private void findProductInStock(RequestStockReserveDTO itemDTO) {
        String url = stockServiceUrl + itemDTO.getSku();
        try {
            log.info("Enviando dados para o estoque: {}", objectMapper.writeValueAsString(itemDTO));
            ResponseEntity<String> response = restTemplate.postForEntity(url, itemDTO, String.class);
            log.info("Resposta do estoque: {}", response.getBody());
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar o objeto RequestStockReserveDTO: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao processar os dados do produto", e);
        } catch (Exception e) {
            log.error("Erro ao buscar produto no estoque: {}", e.getMessage(), e);
            throw new RuntimeException("Produto indisponível ou não encontrado", e);
        }
    }

    private Order createOrderFromDTO(OrderRequestDTO dto) throws JsonProcessingException {
        Order order = new Order();
        order.setClientId(dto.getClientId());
        order.setDtCreate(LocalDateTime.now());
        order.setStatus("ABERTO");

        String itemsJson = objectMapper.writeValueAsString(dto.getItems());
        order.setItems(itemsJson);

        BigDecimal totalAmount = calculateTotalAmount(dto);
        order.setTotalAmount(totalAmount);

        order.setPaymentCardNumber(dto.getPaymentData().getCreditCardNumber());

        return order;
    }

    private BigDecimal calculateTotalAmount(OrderRequestDTO dto) {
        return dto.getItems().stream()
                .map(item -> new BigDecimal(item.getQuantity() * 100))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}