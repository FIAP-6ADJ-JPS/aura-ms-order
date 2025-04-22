package com.postech.auramsorder.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.postech.auramsorder.adapter.dto.RequestStockReserveDTO;
import com.postech.auramsorder.adapter.dto.OrderRequestDTO;
import com.postech.auramsorder.gateway.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class ProcessOrderUseCase {

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public ProcessOrderUseCase(OrderRepository orderRepository, ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    public void processTest(OrderRequestDTO orderRequestDTO) {
        if(orderRequestDTO.getClientId() == null || orderRequestDTO.getItems() == null) {
            throw new IllegalArgumentException("Dados não podem ser nulos");
        }
        findClientById(orderRequestDTO.getClientId().toString());
        //refatorar para pegar todos skus e refatorar endpoint também
        findProductBySku(orderRequestDTO.getItems().getSku());
        RequestStockReserveDTO reserveDTO = new RequestStockReserveDTO();
        reserveDTO.setSku(orderRequestDTO.getItems().getSku());
        reserveDTO.setQuantity(orderRequestDTO.getItems().getQuantity());
        findProductInStock(reserveDTO);
    }

    private void findClientById(String clientId) {
        String url = "http://localhost:8004/api/v1/clients/" + clientId;
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            log.info("Cliente encontrado: {}", response);
        } catch (Exception e) {
            log.error("Erro ao buscar cliente: {}", e.getMessage(), e);
            throw new RuntimeException("Cliente não encontrado", e);
        }
    }

    private void findProductBySku(String sku) {
        String url = "http://localhost:8003/api/v1/products/" + sku;
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            log.info("Produto encontrado: {}", response);
        } catch (Exception e) {
            log.error("Erro ao buscar produto: {}", e.getMessage(), e);
            throw new RuntimeException("Produto não encontrado", e);
        }
    }

    private void findProductInStock(RequestStockReserveDTO itemDTO) {

        String url = "http://localhost:8005/api/v1/stocks/new-reserve";
        try {
            log.info("Enviando dados para o estoque: {}", objectMapper.writeValueAsString(itemDTO));
            ResponseEntity<String> response = restTemplate.postForEntity(url, itemDTO, String.class);
            log.info("Resposta do estoque: {}", response.getBody());
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar o objeto OrderItemDTO: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao processar os dados do produto", e);
        } catch (Exception e) {
            log.error("Erro ao buscar produto no estoque: {}", e.getMessage(), e);
            throw new RuntimeException("Produto indisponível ou não encontrado", e);
        }
    }


//    @Transactional
//    public void process(OrderRequestDTO orderRequestDTO) {
//        log.info("Processando pedido para o cliente: {}", orderRequestDTO.getClientId());
//
//        try {
//            // Converter os dados recebidos para o domínio da Order
//            Order order = createOrderFromDTO(orderRequestDTO);
//
//            // Salvar o pedido inicialmente como "ABERTO"
//            order = orderRepository.save(order);
//
//            log.info("Pedido salvo com o ID: {} e status: {}", order.getId(), order.getStatus());
//
//            // Em um cenário completo, aqui você chamaria os outros serviços:
//            // 1. Chamar serviço de client para verificar se existe e está cadastrado
//            findClientById(orderRequestDTO.getClientId().toString());
//            // 2. Chamar serviço de estoque para verificar disponibilidade
//            // 2. Chamar serviço de pagamento para processar o pagamento
//            // 3. Atualizar o status da ordem com base na resposta
//
//            // Para este exemplo, vamos considerar que tudo deu certo
//            order.setStatus("FECHADO_COM_SUCESSO");
//            orderRepository.save(order);
//
//            log.info("Pedido processado com sucesso. Status final: {}", order.getStatus());
//
//        } catch (Exception e) {
//            log.error("Erro ao processar o pedido: {}", e.getMessage(), e);
//            throw new RuntimeException("Falha ao processar o pedido", e);
//        }
//    }

//    private Order createOrderFromDTO(OrderRequestDTO dto) throws JsonProcessingException {
//        Order order = new Order();
//        order.setClientId(dto.getClientId());
//        order.setDtCreate(LocalDateTime.now());
//        order.setStatus("ABERTO");
//
//        // Convertendo itens para JSON para salvar na coluna items_json
//        String itemsJson = objectMapper.writeValueAsString(dto.getItems());
//        order.setItems(itemsJson);
//
//        // Em um cenário real, você buscaria o preço de cada produto
//        // e calcularia o valor total. Aqui usamos um valor fixo para demonstração.
//        BigDecimal totalAmount = calculateTotalAmount(dto);
//        order.setTotalAmount(totalAmount);
//
//        // Armazenar o número do cartão (em produção, seria necessário criptografar)
//        order.setPaymentCardNumber(dto.getPaymentData().getCreditCardNumber());
//
//        return order;
//    }

//    private BigDecimal calculateTotalAmount(OrderRequestDTO dto) {
//        // Simulando o cálculo - em uma implementação real, você buscaria
//        // o preço de cada produto no microserviço de produtos
//        return dto.getItems().stream()
//                .map(item -> new BigDecimal(item.getQuantity() * 100)) // Preço fictício de 100 por unidade
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//    }


}
