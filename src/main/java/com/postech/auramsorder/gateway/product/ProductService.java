package com.postech.auramsorder.gateway.product;

import com.postech.auramsorder.config.exception.ProductNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ProductService {

    @Value("${product.service.url}")
    private String productServiceUrl;

    private final RestTemplate restTemplate;

    public ProductService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean verifyProduct(String sku) {
        try {
            ResponseEntity<String> response =
                    restTemplate.getForEntity(productServiceUrl + sku, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            throw new ProductNotFoundException("Product not found: " + sku);
        }
    }
}
