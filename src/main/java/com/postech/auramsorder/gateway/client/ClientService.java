package com.postech.auramsorder.gateway.client;

import com.postech.auramsorder.config.exception.ClientNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ClientService {
    @Value("${client.service.url}")
    private String clientServiceUrl;

    private final RestTemplate restTemplate;

    public ClientService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean verifyClient(Long clientId) {
        try {
            ResponseEntity<String> response =
                    restTemplate.getForEntity(clientServiceUrl + clientId, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            throw new ClientNotFoundException("Cliente nao encontradp: " + clientId);
        }
    }
}