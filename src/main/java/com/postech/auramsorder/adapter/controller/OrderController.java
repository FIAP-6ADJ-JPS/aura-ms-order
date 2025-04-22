package com.postech.auramsorder.adapter.controller;

import com.postech.auramsorder.adapter.dto.OrderRequestDTO;
import com.postech.auramsorder.application.ProcessOrderUseCase;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    public final ProcessOrderUseCase processOrderUseCase;

    public OrderController(ProcessOrderUseCase processOrderUseCase) {
        this.processOrderUseCase = processOrderUseCase;
    }

    @PostMapping("/new-solicitation")
    public boolean clientExist(@RequestBody OrderRequestDTO orderRequestDTO) {
        processOrderUseCase.processTest(orderRequestDTO);
        return true;
    }
}
